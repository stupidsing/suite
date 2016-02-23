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
import suite.file.DataFile;
import suite.file.ExtentAllocator.Extent;
import suite.file.PageFile;
import suite.file.SerializedExtentFile;
import suite.file.SerializedPageFile;
import suite.file.impl.ExtentMetadataFileImpl;
import suite.file.impl.SerializedExtentFileImpl;
import suite.file.impl.SerializedPageFileImpl;
import suite.file.impl.SubPageFileImpl;
import suite.immutable.LazyIbTree.Slot;
import suite.primitive.Bytes;
import suite.streamlet.Read;
import suite.util.FunUtil.Sink;
import suite.util.Rethrow;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

public class LazyIbTreeExtentMetadataFilePersister<T> implements Closeable {

	private SerializedPageFile<Integer> nPagesFile;
	private ExtentMetadataFileImpl extentMetadataFile;
	private SerializedExtentFile<PersistSlot<T>> extentFile;
	private Comparator<T> comparator;
	private Serializer<PersistSlot<T>> serializer;

	private Object writeLock = new Object();
	private AtomicInteger nPages;
	private BiMap<Extent, IdentityKey<List<Slot<T>>>> slotsByPointer = new HashBiMap<>();

	public static class PersistSlot<T> {
		public final List<Pair<T, Extent>> pairs;

		public PersistSlot(List<Pair<T, Extent>> pairs) {
			this.pairs = pairs;
		}
	}

	public LazyIbTreeExtentMetadataFilePersister(PageFile pf, Comparator<T> comparator, Serializer<T> ts) {
		Serializer<T> ts1 = Serialize.nullable(ts);
		Serializer<Extent> es = new Serializer<Extent>() {
			public Extent read(DataInput dataInput) throws IOException {
				int start = dataInput.readInt();
				int end = dataInput.readInt();
				return new Extent(start, end);
			}

			public void write(DataOutput dataOutput, Extent value) throws IOException {
				dataOutput.writeByte(value.start);
				dataOutput.writeByte(value.end);
			}
		};
		Serializer<Pair<T, Extent>> ps = Serialize.pair(ts1, es);
		Serializer<List<Pair<T, Extent>>> lps = Serialize.list(ps);
		PageFile pf0 = new SubPageFileImpl(pf, 0, 1);
		PageFile pf1 = new SubPageFileImpl(pf, 1, Integer.MAX_VALUE);

		serializer = new Serializer<PersistSlot<T>>() {
			public PersistSlot<T> read(DataInput dataInput) throws IOException {
				return new PersistSlot<>(lps.read(dataInput));
			}

			public void write(DataOutput dataOutput, PersistSlot<T> value) throws IOException {
				lps.write(dataOutput, value.pairs);
			}
		};

		this.comparator = comparator;
		nPagesFile = new SerializedPageFileImpl<>(pf0, Serialize.int_);
		extentFile = new SerializedExtentFileImpl<>(extentMetadataFile = new ExtentMetadataFileImpl(pf1), serializer);
		nPages = new AtomicInteger(nPagesFile.load(0));
	}

	@Override
	public void close() throws IOException {
		synchronized (writeLock) {
			nPagesFile.save(0, nPages.get());
			extentFile.close();
			nPagesFile.close();
		}
	}

	public LazyIbTree<T> load(Extent extent) {
		return new LazyIbTree<>(comparator, load_(extent));
	}

	public Extent save(LazyIbTree<T> tree) {
		synchronized (writeLock) {
			return save_(tree.root);
		}
	}

	public Map<Extent, Extent> gc(List<Extent> pointers, int back) {
		synchronized (writeLock) {
			int end = nPages.get();
			int start = Math.max(0, end - back);
			List<Extent> extents = extentMetadataFile.scan(start, end);
			Map<Extent, Boolean> isInUse = new HashMap<>();

			Sink<List<Extent>> use = pointers_ -> {
				for (Extent pointer : pointers_)
					if (start <= pointer.start)
						isInUse.put(pointer, Boolean.TRUE);
			};

			use.sink(pointers);

			for (Extent extent : Read.from(extents).reverse())
				if (isInUse.get(extent))
					use.sink(Read.from(extentFile.load(extent).pairs).map(Pair::second).toList());

			Map<Extent, Extent> map = new HashMap<>();
			int pointer = start;

			for (Extent extent0 : extents)
				if (isInUse.get(extent0) == Boolean.TRUE) {
					PersistSlot<T> ps0 = extentFile.load(extent0);
					List<Pair<T, Extent>> pairs0 = ps0.pairs;
					List<Pair<T, Extent>> pairsx = Read.from(pairs0).map(Pair.map1(p -> map.getOrDefault(p, p))).toList();
					PersistSlot<T> psx = new PersistSlot<>(pairsx);
					Extent extent1 = saveFrom(pointer, psx);
					pointer = extent1.end;
					map.put(extent0, extent1);
				}

			nPages.set(pointer);
			slotsByPointer.clear();
			return map;
		}
	}

	private Extent saveFrom(int start, PersistSlot<T> value) {
		Bytes bytes = Rethrow.ioException(() -> Bytes.of(dataOutput -> serializer.write(dataOutput, value)));
		int bs = DataFile.defaultPageSize - 8;
		Extent extent = new Extent(start, start + (bytes.size() + bs - 1) / bs);
		extentMetadataFile.save(extent, bytes);
		return extent;
	}

	private List<Slot<T>> load_(Extent pointer) {
		IdentityKey<List<Slot<T>>> key = slotsByPointer.get(pointer);
		if (key == null) {
			PersistSlot<T> ps = extentFile.load(pointer);
			List<Slot<T>> slots = Read.from(ps.pairs) //
					.map(pair -> new Slot<>(() -> load_(pair.t1), pair.t0)) //
					.toList();
			slotsByPointer.put(pointer, key = IdentityKey.of(slots));
		}
		return key.key;
	}

	private Extent save_(List<Slot<T>> slots) {
		IdentityKey<List<Slot<T>>> key = IdentityKey.of(slots);
		Extent extent = slotsByPointer.inverse().get(key);

		if (extent == null) {
			List<Pair<T, Extent>> pairs = Read.from(slots) //
					.map(slot -> Pair.of(slot.pivot, save_(slot.readSlots()))) //
					.toList();

			int count = 1;
			int pointer = nPages.getAndAdd(count);
			slotsByPointer.put(extent = new Extent(pointer, pointer + count), key);

			extentFile.save(extent, new PersistSlot<>(pairs));
		}

		return extent;
	}

}
