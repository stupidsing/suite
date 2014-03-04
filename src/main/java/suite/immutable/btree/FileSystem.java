package suite.immutable.btree;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import suite.immutable.btree.IbTree.Pointer;
import suite.primitive.Bytes;
import suite.util.FunUtil.Fun;
import suite.util.To;
import suite.util.Util;

public class FileSystem implements Closeable {

	private int pageSize = 4096;
	private FileSystemKeyUtil keyUtil = new FileSystemKeyUtil();

	private List<IbTree<Pointer>> pointerIbTrees = new ArrayList<>();
	private IbTree<Bytes> ibTree;

	public FileSystem(String filename, long capacity) throws FileNotFoundException {
		long nPages = capacity / pageSize;
		IbTreeBuilder builder = new IbTreeBuilder(pageSize / 64, pageSize);

		int i = 0;
		IbTree<Pointer> pointerIbTree;
		pointerIbTrees.add(builder.buildPointerTree(filename + i++));

		while ((pointerIbTree = Util.last(pointerIbTrees)).guaranteedCapacity() < nPages)
			pointerIbTrees.add(builder.buildPointerTree(filename + i++, pointerIbTree));

		ibTree = builder.buildTree(filename + i++, Bytes.comparator, keyUtil.serializer(), pointerIbTree);
	}

	@Override
	public void close() throws IOException {
		ibTree.close();
		ListIterator<IbTree<Pointer>> li = pointerIbTrees.listIterator();
		while (li.hasPrevious())
			li.previous().close();
	}

	public List<Bytes> list(final Bytes start, final Bytes end) {
		return ibTree.holder().transact(new Fun<IbTree<Bytes>.Transaction, List<Bytes>>() {
			public List<Bytes> apply(IbTree<Bytes>.Transaction transaction) {
				return To.list(new FileSystemNameKeySet(transaction).source(start, end));
			}
		});
	}

	public void replace(final Bytes name, final Bytes bytes) {
		ibTree.holder().transact(new Fun<IbTree<Bytes>.Transaction, Object>() {
			public Object apply(IbTree<Bytes>.Transaction transaction) {
				Bytes hash = keyUtil.hash(name);

				try {
					FileSystemNameKeySet ibNameKeySet = new FileSystemNameKeySet(transaction);
					Bytes sizeKey = key(hash, 65, 0);

					Bytes nameBytes0 = ibNameKeySet.source(name, null).source();

					if (Objects.equals(nameBytes0, name)) { // Remove
						int size = toSize(transaction.payload(sizeKey));
						int seq = 0;

						ibNameKeySet.remove(name);
						ibNameKeySet.remove(sizeKey);

						for (int s = 0; s < size; s += pageSize)
							transaction.remove(key(hash, 64, seq++));

					}

					if (bytes != null) { // Create
						int pos = 0, seq = 0, size = bytes.size();

						while (pos < size) {
							int pos1 = Math.max(pos + pageSize, size);
							transaction.replace(key(hash, 64, seq++), bytes.subbytes(pos, pos1 - pos));
						}

						ibNameKeySet.add(name);
						transaction.replace(sizeKey, fromSize(size));
					}

					return null;
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		});
	}

	public void replace(final Bytes name, final int seq, final Bytes bytes) {
		ibTree.holder().transact(new Fun<IbTree<Bytes>.Transaction, Object>() {
			public Object apply(IbTree<Bytes>.Transaction transaction) {
				try {
					transaction.replace(key(keyUtil.hash(name), 64, seq), bytes);
					return null;
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		});
	}

	public void resize(final Bytes name, final int size1) {
		ibTree.holder().transact(new Fun<IbTree<Bytes>.Transaction, Object>() {
			public Object apply(IbTree<Bytes>.Transaction transaction) {
				try {
					Bytes hash = keyUtil.hash(name);
					Bytes sizeKey = key(hash, 65, 0);
					int size0 = toSize(transaction.payload(sizeKey));
					int nPages0 = (size0 + pageSize - 1) % pageSize;
					int nPages1 = (size1 + pageSize - 1) % pageSize;

					for (int page = nPages1; page < nPages0; page++)
						transaction.remove(key(hash, 64, page));
					for (int page = nPages0; page < nPages1; page++)
						transaction.replace(key(hash, 64, page), Bytes.emptyBytes);

					transaction.replace(sizeKey, fromSize(size1));
					return null;
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		});
	}

	private Bytes key(Bytes hash, int id, int seq) {
		return keyUtil.toSeqKey(hash, id, seq).toBytes();
	}

	private Bytes fromSize(int size) {
		return new Bytes(ByteBuffer.allocate(4).putInt(size).array());
	}

	private int toSize(Bytes payload) {
		return ByteBuffer.wrap(payload.getBytes()).asIntBuffer().get();
	}

}
