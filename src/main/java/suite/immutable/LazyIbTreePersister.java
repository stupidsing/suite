package suite.immutable;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import suite.adt.BiMap;
import suite.adt.HashBiMap;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.SerializedPageFilePersister;
import suite.immutable.LazyIbTree.Slot;
import suite.streamlet.Read;
import suite.util.SerializeUtil;
import suite.util.SerializeUtil.Serializer;

public class LazyIbTreePersister<T> implements Closeable {

	private SerializedPageFilePersister<PersistSlot<T>> persister;

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
				return new PersistSlot<T>(pageNos, pivot);
			}

			public void write(DataOutput dataOutput, PersistSlot<T> value) throws IOException {
				pageNosSerializer.write(dataOutput, value.pageNos);
				ts.write(dataOutput, value.pivot);
			}
		};

		SerializedPageFile<PersistSlot<T>> pageFile = new SerializedPageFile<>(pf, serializer);
		persister = new SerializedPageFilePersister<>(pageFile);
	}

	@Override
	public void close() {
		persister.close();
	}

	public LazyIbTree<T> load(List<Integer> pageNos) {
		return new LazyIbTree<T>(comparator, () -> load_(pageNos));
	}

	public List<Integer> save(LazyIbTree<T> tree) {
		return save_(tree.root());
	}

	private List<Slot<T>> load_(List<Integer> pageNos) {
		return Read.from(pageNos).map(pageNo -> {
			if (pageNo != 0) {
				Slot<T> slot = slotByPageNo.inverse().get(pageNo);
				if (slot == null) {
					PersistSlot<T> ps = persister.load(pageNo);
					slotByPageNo.put(slot = new Slot<T>(() -> load_(ps.pageNos), ps.pivot), pageNo);
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
					slotByPageNo.put(slot, pageNo = persister.save(new PersistSlot<T>(pageNos, slot.pivot)));
				}
				return pageNo;
			} else
				return 0;
		}).toList();
	}

}
