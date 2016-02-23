package suite.immutable;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.BiMap;
import suite.adt.HashBiMap;
import suite.adt.IdentityKey;
import suite.adt.Pair;
import suite.file.ExtentAllocator.Extent;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.impl.ExtentMetadataFileImpl;
import suite.file.impl.SerializedPageFileImpl;
import suite.file.impl.SubPageFileImpl;
import suite.immutable.LazyIbTree.Slot;
import suite.primitive.Bytes;
import suite.streamlet.Read;
import suite.util.FunUtil.Sink;
import suite.util.Rethrow;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

public class LazyIbTreeExtentMetadataFilePersister<T> implements LazyIbTreePersister<Extent, T> {

	private SerializedPageFile<Integer> nPagesFile;
	private ExtentMetadataFileImpl extentMetadataFile;
	private Comparator<T> comparator;
	private Serializer<PersistSlot<T>> serializer;

	private Object writeLock = new Object();
	private int nPages;
	private BiMap<Extent, IdentityKey<List<Slot<T>>>> slotsByExtent = new HashBiMap<>();

	public static class PersistSlot<T> {
		public final List<Pair<T, Extent>> pairs;

		public PersistSlot(List<Pair<T, Extent>> pairs) {
			this.pairs = pairs;
		}
	}

	public LazyIbTreeExtentMetadataFilePersister(PageFile pf, Comparator<T> comparator, Serializer<T> ts) {
		Serializer<T> ts1 = Serialize.nullable(ts);
		Serializer<Extent> es = Serialize.extent();
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
		extentMetadataFile = new ExtentMetadataFileImpl(pf1);
		nPages = nPagesFile.load(0);
	}

	@Override
	public void close() throws IOException {
		synchronized (writeLock) {
			nPagesFile.save(0, nPages);
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

	public Map<Extent, Extent> gc(List<Extent> roots, int back) {
		synchronized (writeLock) {
			int end = nPages;
			int start = Math.max(0, end - back);
			List<Extent> extents = extentMetadataFile.scan(start, end);
			Set<Extent> isInUse = new HashSet<>();

			Sink<List<Extent>> use = extents_ -> {
				for (Extent extent : extents_)
					if (start <= extent.start)
						isInUse.add(extent);
			};

			use.sink(roots);

			for (Extent extent : Read.from(extents).reverse())
				if (isInUse.contains(extent))
					use.sink(Read.from(load0(extent).pairs).map(Pair::second).toList());

			Map<Extent, Extent> map = new HashMap<>();
			int pointer = start;

			for (Extent extent0 : extents)
				if (isInUse.contains(extent0)) {
					PersistSlot<T> ps0 = load0(extent0);
					List<Pair<T, Extent>> pairs0 = ps0.pairs;
					List<Pair<T, Extent>> pairsx = Read.from(pairs0).map(Pair.map1(p -> map.getOrDefault(p, p))).toList();
					PersistSlot<T> psx = new PersistSlot<>(pairsx);
					Extent extent1 = save0(pointer, psx);
					pointer = extent1.end;
					map.put(extent0, extent1);
				}

			nPages = pointer;
			slotsByExtent.clear();
			return map;
		}
	}

	private List<Slot<T>> load_(Extent extent) {
		IdentityKey<List<Slot<T>>> key = slotsByExtent.get(extent);
		if (key == null) {
			PersistSlot<T> ps = load0(extent);
			List<Slot<T>> slots = Read.from(ps.pairs) //
					.map(pair -> new Slot<>(() -> load_(pair.t1), pair.t0)) //
					.toList();
			slotsByExtent.put(extent, key = IdentityKey.of(slots));
		}
		return key.key;
	}

	private Extent save_(List<Slot<T>> slots) {
		IdentityKey<List<Slot<T>>> key = IdentityKey.of(slots);
		Extent extent = slotsByExtent.inverse().get(key);
		if (extent == null) {
			List<Pair<T, Extent>> pairs = Read.from(slots) //
					.map(slot -> Pair.of(slot.pivot, save_(slot.readSlots()))) //
					.toList();
			slotsByExtent.put(extent = save0(nPages, new PersistSlot<>(pairs)), key);
			nPages = extent.end;
		}
		return extent;
	}

	private PersistSlot<T> load0(Extent extent) {
		return Rethrow.ioException(() -> serializer.read(new DataInputStream(extentMetadataFile.load(extent).asInputStream())));
	}

	private Extent save0(int start, PersistSlot<T> value) {
		int bs = ExtentMetadataFileImpl.blockSize;
		Bytes bytes = Rethrow.ioException(() -> Bytes.of(dataOutput -> serializer.write(dataOutput, value)));
		Extent extent = new Extent(start, start + (bytes.size() + bs - 1) / bs);
		extentMetadataFile.save(extent, bytes);
		return extent;
	}

}
