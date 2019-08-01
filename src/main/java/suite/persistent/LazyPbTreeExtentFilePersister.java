package suite.persistent;

import static java.lang.Math.max;
import static primal.statics.Rethrow.ex;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import primal.adt.IdentityKey;
import primal.adt.Pair;
import primal.fp.Funs.Sink;
import suite.adt.map.BiHashMap;
import suite.adt.map.BiMap;
import suite.file.ExtentAllocator.Extent;
import suite.file.ExtentFile;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.impl.FileFactory;
import suite.file.impl.SerializedFileFactory;
import suite.node.util.Singleton;
import suite.persistent.LazyPbTree.Slot;
import suite.serialize.SerInput;
import suite.serialize.SerOutput;
import suite.serialize.Serialize;
import suite.serialize.Serialize.Serializer;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.To;

public class LazyPbTreeExtentFilePersister<T> implements LazyPbTreePersister<Extent, T> {

	private Serialize ser = Singleton.me.serialize;

	private SerializedPageFile<Integer> nPagesFile;
	private ExtentFile extentFile;
	private Comparator<T> comparator;
	private Serializer<PersistSlot<T>> serializer;

	private Object writeLock = new Object();
	private int nPages;
	private BiMap<Extent, IdentityKey<List<Slot<T>>>> slotsByExtent = new BiHashMap<>();

	public static class PersistSlot<T> {
		public final List<Pair<T, Extent>> pairs;

		public PersistSlot(List<Pair<T, Extent>> pairs) {
			this.pairs = pairs;
		}
	}

	public LazyPbTreeExtentFilePersister(PageFile pf, Comparator<T> comparator, Serializer<T> ts) {
		var ts1 = ser.nullable(ts);
		var es = ser.extent();
		var ps = ser.pair(ts1, es);
		var lps = ser.list(ps);

		serializer = new Serializer<>() {
			public PersistSlot<T> read(SerInput si) throws IOException {
				return new PersistSlot<>(lps.read(si));
			}

			public void write(SerOutput so, PersistSlot<T> value) throws IOException {
				lps.write(so, value.pairs);
			}
		};

		var pfs = FileFactory.subPageFiles(pf, 0, 1, Integer.MAX_VALUE);

		this.comparator = comparator;
		nPagesFile = SerializedFileFactory.serialized(pfs[0], ser.int_);
		extentFile = FileFactory.extentFile(pfs[1]);
		nPages = nPagesFile.load(0);
	}

	@Override
	public void close() throws IOException {
		synchronized (writeLock) {
			nPagesFile.save(0, nPages);
			nPagesFile.close();
		}
	}

	@Override
	public LazyPbTree<T> load(Extent extent) {
		return new LazyPbTree<>(comparator, load_(extent));
	}

	@Override
	public Extent save(LazyPbTree<T> tree) {
		synchronized (writeLock) {
			return save_(tree.root);
		}
	}

	@Override
	public Map<Extent, Extent> gc(List<Extent> roots, int back) {
		synchronized (writeLock) {
			var end = nPages;
			var start = max(0, end - back);
			var isInUse = new HashSet<>();

			Sink<List<Extent>> use = extents_ -> {
				for (var extent : extents_)
					if (start <= extent.start)
						isInUse.add(extent);
			};

			use.f(roots);

			var extents = extentFile.scan(start, end);

			for (var extent : Read.from(extents).reverse())
				if (isInUse.contains(extent))
					use.f(Read.from(loadSlot(extent).pairs).map(Pair::snd).toList());

			var map = new HashMap<Extent, Extent>();

			if (!extents.isEmpty()) {
				var pointer = extents.get(0).start;

				for (var extent0 : extents)
					if (isInUse.contains(extent0)) {
						var ps0 = loadSlot(extent0);
						var pairs0 = ps0.pairs;
						var pairsx = Read.from(pairs0).map(Pair.mapSnd(p -> map.getOrDefault(p, p))).toList();
						var psx = new PersistSlot<>(pairsx);
						var extentx = saveSlot(pointer, psx);
						pointer = extentx.end;
						map.put(extent0, extentx);
					}

				nPages = pointer;
				slotsByExtent.clear();
			}

			return map;
		}
	}

	private List<Slot<T>> load_(Extent extent) {
		var key = slotsByExtent.get(extent);
		if (key == null) {
			var ps = loadSlot(extent);
			var slots = Read.from2(ps.pairs).map((k, v) -> new Slot<>(() -> load_(v), k)).toList();
			slotsByExtent.put(extent, key = IdentityKey.of(slots));
		}
		return key.key;
	}

	private Extent save_(List<Slot<T>> slots) {
		var key = IdentityKey.of(slots);
		var extent = slotsByExtent.inverse().get(key);
		if (extent == null) {
			var pairs = Read.from(slots).map(slot -> Pair.of(slot.pivot, save_(slot.readSlots()))).toList();
			slotsByExtent.put(extent = saveSlot(nPages, new PersistSlot<>(pairs)), key);
			nPages = extent.end;
		}
		return extent;
	}

	private PersistSlot<T> loadSlot(Extent extent) {
		return ex(() -> serializer.read(SerInput.of(extentFile.load(extent).collect(As::inputStream))));
	}

	private Extent saveSlot(int start, PersistSlot<T> value) {
		var bs = ExtentFile.blockSize;
		var bytes = To.bytes(so -> serializer.write(so, value));
		var extent = new Extent(start, start + (bytes.size() + bs - 1) / bs);
		extentFile.save(extent, bytes);
		return extent;
	}

}
