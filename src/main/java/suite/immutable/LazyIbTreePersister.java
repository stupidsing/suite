package suite.immutable;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import suite.adt.BiMap;
import suite.adt.HashBiMap;
import suite.file.SerializedPageFile;
import suite.immutable.LazyIbTree.Slot;
import suite.streamlet.Read;

public class LazyIbTreePersister<T> {

	private AtomicInteger nPages = new AtomicInteger(1);
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

	public LazyIbTreePersister(Comparator<T> comparator) {
		this.comparator = comparator;
	}

	public LazyIbTree<T> load(PersistSlot<T> ps) {
		return new LazyIbTree<T>(comparator, () -> load(ps.pageNos));
	}

	public PersistSlot<T> save(LazyIbTree<T> tree) {
		return new PersistSlot<T>(save(tree.root()), null);
	}

	private List<Slot<T>> load(List<Integer> pageNos) {
		return Read.from(pageNos).map(pageNo -> {
			if (pageNo != 0) {
				Slot<T> slot = slotByPageNo.inverse().get(pageNo);
				if (slot == null) {
					PersistSlot<T> ps = pageFile.load(pageNo);
					slotByPageNo.put(slot = new Slot<T>(() -> load(ps.pageNos), ps.pivot), pageNo);
				}
				return slot;
			} else
				return null;
		}).toList();
	}

	private List<Integer> save(List<Slot<T>> slots) {
		return Read.from(slots).map(slot -> {
			if (slot != null) {
				Integer pageNo = slotByPageNo.get(slot);
				if (pageNo == null) {
					List<Integer> pageNos = save(slot.readSlots());
					slotByPageNo.put(slot, pageNo = nPages.incrementAndGet());
					pageFile.save(pageNo, new PersistSlot<T>(pageNos, slot.pivot));
				}
				return pageNo;
			} else
				return 0;
		}).toList();
	}

}
