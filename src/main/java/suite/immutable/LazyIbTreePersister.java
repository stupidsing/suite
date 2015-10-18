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
import suite.file.impl.SerializedPageFile;
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
	private BiMap<Slot<T>, Integer> slotByPageNo = new HashBiMap<>();

	public static class PersistSlot<T> {
		public final List<Integer> pageNos;
		public final T pivot;

		public PersistSlot(List<Integer> pageNos, T pivot) {
			this.pageNos = pageNos;
			this.pivot = pivot;
		}
	}

	public LazyIbTreePersister(PageFile pf, Comparator<T> comparator, Serializer<T> ts) {
		this.comparator = comparator;

		Serializer<List<Integer>> pageNosSerializer = SerializeUtil.list(SerializeUtil.intSerializer);

		Serializer<PersistSlot<T>> serializer = new Serializer<PersistSlot<T>>() {
			public PersistSlot<T> read(DataInput dataInput) throws IOException {
				List<Integer> pageNos = pageNosSerializer.read(dataInput);
				T pivot = ts.read(dataInput);
				return new PersistSlot<>(pageNos, pivot);
			}

			public void write(DataOutput dataOutput, PersistSlot<T> value) throws IOException {
				pageNosSerializer.write(dataOutput, value.pageNos);
				ts.write(dataOutput, value.pivot);
			}
		};

		pageFile = new SerializedPageFile<>(pf, serializer);
	}

	@Override
	public void close() {
		pageFile.close();
	}

	public LazyIbTree<T> load(List<Integer> pageNos) {
		return new LazyIbTree<>(comparator, () -> load_(pageNos));
	}

	public List<Integer> save(LazyIbTree<T> tree) {
		synchronized (writeLock) {
			return save_(tree.root());
		}
	}

	public List<Integer> gc(List<Integer> pageNos, int back) {
		synchronized (writeLock) {
			int end = nPages.get();
			int start = Math.min(0, end - back);
			boolean isInUse[] = new boolean[end - start];

			Sink<List<Integer>> use = pageNos_ -> {
				for (int pageNo : pageNos_)
					if (pageNo >= start)
						isInUse[pageNo - start] = true;
			};

			use.sink(pageNos);

			for (int pageNo = end - 1; pageNo >= start; pageNo--)
				if (isInUse[pageNo - start])
					use.sink(pageFile.load(pageNo).pageNos);

			Map<Integer, Integer> map = new HashMap<>();
			int p1 = start;

			for (int p0 = start; p0 < end; p0++)
				if (isInUse[p0]) {
					pageFile.save(p1, pageFile.load(p0));
					map.put(p0, p1++);
				} else
					slotByPageNo.remove(p0);

			return Read.from(pageNos).map(map::get).toList();
		}
	}

	private List<Slot<T>> load_(List<Integer> pageNos) {
		return Read.from(pageNos).map(pageNo -> {
			if (pageNo != 0) {
				Slot<T> slot = slotByPageNo.inverse().get(pageNo);
				if (slot == null) {
					PersistSlot<T> ps = pageFile.load(pageNo);
					slotByPageNo.put(slot = new Slot<>(() -> load_(ps.pageNos), ps.pivot), pageNo);
				}
				return slot;
			} else
				return null;
		}).toList();
	}

	private List<Integer> save_(List<Slot<T>> slots) {
		return Read.from(slots).map(slot -> {
			if (slot != null) {
				Integer pageNo = slotByPageNo.get(slot);
				if (pageNo == null) {
					List<Integer> pageNos = save_(slot.readSlots());
					slotByPageNo.put(slot, pageNo = nPages.incrementAndGet());
					pageFile.save(pageNo, new PersistSlot<>(pageNos, slot.pivot));
				}
				return pageNo;
			} else
				return 0;
		}).toList();
	}

}
