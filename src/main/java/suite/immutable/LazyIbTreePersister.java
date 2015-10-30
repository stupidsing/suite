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
	private BiMap<Integer, Slot<T>> slotByPointer = new HashBiMap<>();

	public static class PersistSlot<T> {
		public final List<Integer> pointers;
		public final T pivot;

		public PersistSlot(List<Integer> pointers, T pivot) {
			this.pointers = pointers;
			this.pivot = pivot;
		}
	}

	public LazyIbTreePersister(PageFile pf, Comparator<T> comparator, Serializer<T> ts) {
		this.comparator = comparator;

		Serializer<List<Integer>> pointersSerializer = SerializeUtil.list(SerializeUtil.intSerializer);
		Serializer<T> ts1 = SerializeUtil.nullable(ts);
		Serializer<PersistSlot<T>> serializer = new Serializer<PersistSlot<T>>() {
			public PersistSlot<T> read(DataInput dataInput) throws IOException {
				List<Integer> pointers = pointersSerializer.read(dataInput);
				T pivot = ts1.read(dataInput);
				return new PersistSlot<>(pointers, pivot);
			}

			public void write(DataOutput dataOutput, PersistSlot<T> value) throws IOException {
				pointersSerializer.write(dataOutput, value.pointers);
				ts1.write(dataOutput, value.pivot);
			}
		};

		pageFile = new SerializedPageFileImpl<>(pf, serializer);
	}

	@Override
	public void close() throws IOException {
		pageFile.close();
	}

	public LazyIbTree<T> load(List<Integer> pointers) {
		return new LazyIbTree<>(comparator, () -> load_(pointers));
	}

	public List<Integer> save(LazyIbTree<T> tree) {
		synchronized (writeLock) {
			return save_(tree.root());
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
					use.sink(pageFile.load(pointer).pointers);

			Map<Integer, Integer> map = new HashMap<>();
			int p1 = start;

			for (int p0 = start; p0 < end; p0++)
				if (isInUse[p0]) {
					pageFile.save(p1, pageFile.load(p0));
					map.put(p0, p1++);
				} else
					slotByPointer.remove(p0);

			return Read.from(pointers).map(map::get).toList();
		}
	}

	private List<Slot<T>> load_(List<Integer> pointers) {
		return Read.from(pointers).map(pointer -> {
			Slot<T> slot = slotByPointer.get(pointer);
			if (slot == null) {
				PersistSlot<T> ps = pageFile.load(pointer);
				slotByPointer.put(pointer, slot = new Slot<>(() -> load_(ps.pointers), ps.pivot));
			}
			return slot;
		}).toList();
	}

	private List<Integer> save_(List<Slot<T>> slots) {
		return Read.from(slots).map(slot -> {
			Integer pointer = slotByPointer.inverse().get(slot);
			if (pointer == null) {
				List<Integer> pointers = save_(slot.readSlots());
				slotByPointer.put(pointer = nPages.getAndIncrement(), slot);
				pageFile.save(pointer, new PersistSlot<>(pointers, slot.pivot));
			}
			return pointer;
		}).toList();
	}

}
