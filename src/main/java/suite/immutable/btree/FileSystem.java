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
import suite.primitive.Bytes.BytesBuilder;
import suite.util.To;
import suite.util.Util;

public class FileSystem implements Closeable {

	private static final byte DATAID = 64;
	private static final byte SIZEID = 65;

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

	public void create() {
		ibTree.holder().commit(ibTree.create());
	}

	public Bytes read(final Bytes name) {
		IbTree<Bytes>.Transaction transaction = ibTree.holder().begin();
		Bytes hash = keyUtil.hash(name);
		Bytes payload = transaction.get(keyUtil.toSeqKey(hash, SIZEID, 0).toBytes());

		if (payload != null) {
			int seq = 0, size = toSize(payload);
			BytesBuilder bb = new BytesBuilder();

			for (int s = 0; s < size; s += pageSize)
				bb.append(transaction.get(key(hash, DATAID, seq++)));

			return bb.toBytes();
		} else
			return null;
	}

	public List<Bytes> list(final Bytes start, final Bytes end) {
		IbTree<Bytes>.Transaction transaction = ibTree.holder().begin();
		return To.list(new FileSystemNameKeySet(transaction).list(start, end));
	}

	public void replace(final Bytes name, final Bytes bytes) {
		IbTree<Bytes>.Transaction transaction = ibTree.holder().begin();
		FileSystemNameKeySet ibNameKeySet = new FileSystemNameKeySet(transaction);
		Bytes hash = keyUtil.hash(name);
		Bytes sizeKey = key(hash, SIZEID, 0);

		Bytes nameBytes0 = ibNameKeySet.list(name, null).source();

		if (Objects.equals(nameBytes0, name)) { // Remove
			int seq = 0, size = toSize(transaction.get(sizeKey));

			ibNameKeySet.remove(name);
			transaction.remove(sizeKey);
			for (int s = 0; s < size; s += pageSize)
				transaction.remove(key(hash, DATAID, seq++));
		}

		if (bytes != null) { // Create
			int pos = 0, seq = 0, size = bytes.size();

			while (pos < size) {
				int pos1 = Math.min(pos + pageSize, size);
				transaction.replace(key(hash, DATAID, seq++), bytes.subbytes(pos, pos1));
				pos = pos1;
			}
			transaction.replace(sizeKey, fromSize(size));
			ibNameKeySet.add(name);
		}

		ibTree.holder().commit(transaction);
	}

	public void replace(final Bytes name, final int seq, final Bytes bytes) {
		IbTree<Bytes>.Transaction transaction = ibTree.holder().begin();
		transaction.replace(key(keyUtil.hash(name), DATAID, seq), bytes);
		ibTree.holder().commit(transaction);
	}

	public void resize(final Bytes name, final int size1) {
		IbTree<Bytes>.Transaction transaction = ibTree.holder().begin();
		Bytes hash = keyUtil.hash(name);
		Bytes sizeKey = key(hash, SIZEID, 0);
		int size0 = toSize(transaction.get(sizeKey));
		int nPages0 = (size0 + pageSize - 1) % pageSize;
		int nPages1 = (size1 + pageSize - 1) % pageSize;

		for (int page = nPages1; page < nPages0; page++)
			transaction.remove(key(hash, DATAID, page));
		for (int page = nPages0; page < nPages1; page++)
			transaction.replace(key(hash, DATAID, page), Bytes.emptyBytes);

		transaction.replace(sizeKey, fromSize(size1));
		ibTree.holder().commit(transaction);
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
