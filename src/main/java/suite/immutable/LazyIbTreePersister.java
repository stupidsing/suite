package suite.immutable;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import suite.adt.BiMap;
import suite.adt.HashBiMap;
import suite.adt.IdentityKey;
import suite.adt.Pair;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.impl.SerializedPageFileImpl;
import suite.immutable.LazyIbTree.Slot;
import suite.streamlet.Read;
import suite.util.FunUtil.Sink;
import suite.util.SerializeUtil;
import suite.util.SerializeUtil.Serializer;

public class LazyIbTreePersister<T> implements Closeable {

	private AtomicInteger nPages = new AtomicInteger(1);
	private Object writeLock = new Object();

	private SerializedPageFile<PersistSlot<T>> pageFile;
	private Comparator<T> comparator;
	private BiMap<Integer, IdentityKey<List<Slot<T>>>> slotsByPointer = new HashBiMap<>();

	public static class PersistSlot<T> {
		public final List<Pair<T, Integer>> pairs;

		public PersistSlot(List<Pair<T, Integer>> pairs) {
			this.pairs = pairs;
		}
	}

	public LazyIbTreePersister(PageFile pf, Comparator<T> comparator, Serializer<T> ts) {
		this.comparator = comparator;

		Serializer<T> ts1 = SerializeUtil.nullable(ts);
		Serializer<Pair<T, Integer>> ps = SerializeUtil.pair(ts1, SerializeUtil.intSerializer);
		Serializer<List<Pair<T, Integer>>> lps = SerializeUtil.list(ps);
		Serializer<PersistSlot<T>> pss = new Serializer<PersistSlot<T>>() {
			public PersistSlot<T> read(DataInput dataInput) throws IOException {
				return new PersistSlot<>(lps.read(dataInput));
			}

			public void write(DataOutput dataOutput, PersistSlot<T> value) throws IOException {
				lps.write(dataOutput, value.pairs);
			}
		};

		pageFile = new SerializedPageFileImpl<>(pf, pss);
	}

	@Override
	public void close() throws IOException {
		pageFile.close();
	}

	public LazyIbTree<T> load(Integer pointer) {
		return new LazyIbTree<>(comparator, load_(pointer));
	}

	public Integer save(LazyIbTree<T> tree) {
		synchronized (writeLock) {
			return save_(tree.root);
		}
	}

	public List<Integer> gc(List<Integer> pointers, int back) {
		synchronized (writeLock) {
			int end = nPages.get();
			int start = Math.max(0, end - back);
			boolean isInUse[] = new boolean[end - start];

			Sink<List<Integer>> use = pointers_ -> {
				for (int pointer : pointers_)
					if (pointer >= start)
						isInUse[pointer - start] = true;
			};

			use.sink(pointers);

			for (int pointer = end - 1; pointer >= start; pointer--)
				if (isInUse[pointer - start])
					use.sink(Read.from(pageFile.load(pointer).pairs).map(pair -> pair.t1).toList());

			Map<Integer, Integer> map = new HashMap<>();
			int p1 = start;

			for (int p0 = start; p0 < end; p0++)
				if (isInUse[p0]) {
					pageFile.save(p1, pageFile.load(p0));
					map.put(p0, p1++);
				} else
					slotsByPointer.remove(p0);

			return Read.from(pointers).map(map::get).toList();
		}
	}

	private List<Slot<T>> load_(Integer pointer) {
		IdentityKey<List<Slot<T>>> key = slotsByPointer.get(pointer);
		if (key == null) {
			PersistSlot<T> ps = pageFile.load(pointer);
			List<Slot<T>> slots = Read.from(ps.pairs) //
					.map(pair -> new Slot<>(() -> load_(pair.t1), pair.t0)) //
					.toList();
			slotsByPointer.put(pointer, key = IdentityKey.of(slots));
		}
		return key.key;
	}

	private Integer save_(List<Slot<T>> slots) {
		IdentityKey<List<Slot<T>>> key = IdentityKey.of(slots);
		Integer pointer = slotsByPointer.inverse().get(key);
		if (pointer == null) {
			List<Pair<T, Integer>> pairs = Read.from(slots) //
					.map(slot -> Pair.of(slot.pivot, save_(slot.readSlots()))) //
					.toList();
			slotsByPointer.put(pointer = nPages.getAndIncrement(), key);
			pageFile.save(pointer, new PersistSlot<>(pairs));
		}
		return pointer;
	}

}
