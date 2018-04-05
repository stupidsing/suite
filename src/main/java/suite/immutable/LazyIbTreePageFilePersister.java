package suite.immutable;

import static suite.util.Friends.max;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.IdentityKey;
import suite.adt.map.BiMap;
import suite.adt.map.HashBiMap;
import suite.adt.pair.Pair;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.impl.FileFactory;
import suite.file.impl.SerializedFileFactory;
import suite.immutable.LazyIbTree.Slot;
import suite.streamlet.Read;
import suite.util.DataInput_;
import suite.util.DataOutput_;
import suite.util.FunUtil.Sink;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

public class LazyIbTreePageFilePersister<T> implements LazyIbTreePersister<Integer, T> {

	private Serialize serialize = Serialize.me;
	private SerializedPageFile<Integer> nPagesFile;
	private SerializedPageFile<PersistSlot<T>> pageFile;
	private Comparator<T> comparator;
	private Object writeLock = new Object();
	private int nPages;
	private BiMap<Integer, IdentityKey<List<Slot<T>>>> slotsByPointer = new HashBiMap<>();

	public static class PersistSlot<T> {
		public final List<Pair<T, Integer>> pairs;

		public PersistSlot(List<Pair<T, Integer>> pairs) {
			this.pairs = pairs;
		}
	}

	public LazyIbTreePageFilePersister(PageFile pf, Comparator<T> comparator, Serializer<T> ts) {
		Serializer<T> ts1 = serialize.nullable(ts);
		Serializer<Pair<T, Integer>> ps = serialize.pair(ts1, serialize.int_);
		Serializer<List<Pair<T, Integer>>> lps = serialize.list(ps);
		Serializer<PersistSlot<T>> pss = new Serializer<>() {
			public PersistSlot<T> read(DataInput_ dataInput) throws IOException {
				return new PersistSlot<>(lps.read(dataInput));
			}

			public void write(DataOutput_ dataOutput, PersistSlot<T> value) throws IOException {
				lps.write(dataOutput, value.pairs);
			}
		};

		PageFile[] pfs = FileFactory.subPageFiles(pf, 0, 1, Integer.MAX_VALUE);

		this.comparator = comparator;
		nPagesFile = SerializedFileFactory.serialized(pfs[0], serialize.int_);
		pageFile = SerializedFileFactory.serialized(pfs[1], pss);
		nPages = nPagesFile.load(0);
	}

	@Override
	public void close() throws IOException {
		synchronized (writeLock) {
			nPagesFile.save(0, nPages);
			pageFile.close();
			nPagesFile.close();
		}
	}

	public LazyIbTree<T> load(Integer pointer) {
		return new LazyIbTree<>(comparator, load_(pointer));
	}

	public Integer save(LazyIbTree<T> tree) {
		synchronized (writeLock) {
			return save_(tree.root);
		}
	}

	@Override
	public Map<Integer, Integer> gc(List<Integer> pointers, int back) {
		synchronized (writeLock) {
			var end = nPages;
			int start = max(0, end - back);
			var isInUse = new boolean[end - start];

			Sink<List<Integer>> use = pointers_ -> {
				for (var pointer : pointers_)
					if (start <= pointer)
						isInUse[pointer - start] = true;
			};

			use.sink(pointers);

			for (var pointer = end - 1; start <= pointer; pointer--)
				if (isInUse[pointer - start])
					use.sink(Read.from(pageFile.load(pointer).pairs).map(Pair::second).toList());

			var map = new HashMap<Integer, Integer>();
			var p1 = start;

			for (var p0 = start; p0 < end; p0++)
				if (isInUse[p0]) {
					PersistSlot<T> ps0 = pageFile.load(p0);
					var pairs0 = ps0.pairs;
					List<Pair<T, Integer>> pairsx = Read.from(pairs0).map(Pair.map1(p -> map.getOrDefault(p, p))).toList();
					PersistSlot<T> psx = new PersistSlot<>(pairsx);
					pageFile.save(p1, psx);
					map.put(p0, p1++);
				}

			nPages = p1;
			slotsByPointer.clear();
			return map;
		}
	}

	private List<Slot<T>> load_(Integer pointer) {
		IdentityKey<List<Slot<T>>> key = slotsByPointer.get(pointer);
		if (key == null) {
			PersistSlot<T> ps = pageFile.load(pointer);
			var slots = Read //
					.from(ps.pairs) //
					.map(pair -> new Slot<>(() -> load_(pair.t1), pair.t0)) //
					.toList();
			slotsByPointer.put(pointer, key = IdentityKey.of(slots));
		}
		return key.key;
	}

	private Integer save_(List<Slot<T>> slots) {
		IdentityKey<List<Slot<T>>> key = IdentityKey.of(slots);
		var pointer = slotsByPointer.inverse().get(key);
		if (pointer == null) {
			var pairs = Read //
					.from(slots) //
					.map(slot -> Pair.of(slot.pivot, save_(slot.readSlots()))) //
					.toList();
			slotsByPointer.put(pointer = nPages++, key);
			pageFile.save(pointer, new PersistSlot<>(pairs));
		}
		return pointer;
	}

}
