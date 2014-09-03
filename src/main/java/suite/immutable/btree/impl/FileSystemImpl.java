package suite.immutable.btree.impl;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import suite.immutable.btree.FileSystem;
import suite.immutable.btree.IbTree;
import suite.immutable.btree.IbTreeMutator;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.To;
import suite.util.Util;

public class FileSystemImpl implements FileSystem {

	private static byte DATAID = 64;
	private static byte SIZEID = 65;

	private int pageSize = 4096;
	private FileSystemKeyUtil keyUtil = new FileSystemKeyUtil();

	private List<IbTreeImpl<Integer>> pointerIbTrees = new ArrayList<>();
	private IbTree<Bytes> ibTree;

	public FileSystemImpl(String filename, long capacity) throws FileNotFoundException {
		long nPages = capacity / pageSize;
		IbTreeBuilder builder = new IbTreeBuilder(pageSize / 64, pageSize);

		int i = 0;
		IbTreeImpl<Integer> pointerIbTree;
		pointerIbTrees.add(builder.buildPointerTree(filename + i++));

		while ((pointerIbTree = Util.last(pointerIbTrees)).guaranteedCapacity() < nPages)
			pointerIbTrees.add(builder.buildPointerTree(filename + i++, pointerIbTree));

		ibTree = builder.buildTree(filename + i++, Bytes.comparator, keyUtil.serializer(), pointerIbTree);
	}

	@Override
	public void close() {
		ibTree.close();
		ListIterator<IbTreeImpl<Integer>> li = pointerIbTrees.listIterator();
		while (li.hasPrevious())
			li.previous().close();
	}

	@Override
	public void create() {
		ibTree.create().commit();
	}

	@Override
	public Bytes read(Bytes name) {
		IbTreeMutator<Bytes> mutator = ibTree.begin();
		Bytes hash = keyUtil.hash(name);
		Integer size = mutator.getData(key(hash, SIZEID, 0));

		if (size != null) {
			int seq = 0;
			BytesBuilder bb = new BytesBuilder();
			for (int s = 0; s < size; s += pageSize)
				bb.append(mutator.getPayload(key(hash, DATAID, seq++)));
			return bb.toBytes().subbytes(0, size);
		} else
			return null;
	}

	@Override
	public List<Bytes> list(Bytes start, Bytes end) {
		IbTreeMutator<Bytes> mutator = ibTree.begin();
		return To.list(new FileSystemNameKeySet(mutator).list(start, end));
	}

	@Override
	public void replace(Bytes name, Bytes bytes) {
		IbTreeMutator<Bytes> mutator = ibTree.begin();
		FileSystemNameKeySet fsNameKeySet = new FileSystemNameKeySet(mutator);
		Bytes hash = keyUtil.hash(name);
		Bytes sizeKey = key(hash, SIZEID, 0);

		Bytes nameBytes0 = fsNameKeySet.list(name, null).source();
		boolean isRemove = Objects.equals(nameBytes0, name);
		boolean isCreate = bytes != null;

		if (isRemove) {
			int seq = 0, size = mutator.getData(sizeKey);

			if (!isCreate)
				fsNameKeySet.remove(name);
			mutator.remove(sizeKey);
			for (int s = 0; s < size; s += pageSize)
				mutator.remove(key(hash, DATAID, seq++));
		}

		if (isCreate) {
			int pos = 0, seq = 0, size = bytes.size();

			while (pos < size) {
				int pos1 = Math.min(pos + pageSize, size);
				mutator.put(key(hash, DATAID, seq++), bytes.subbytes(pos, pos1));
				pos = pos1;
			}
			mutator.put(sizeKey, size);
			if (!isRemove)
				fsNameKeySet.add(name);
		}

		mutator.commit();
	}

	@Override
	public void replace(Bytes name, int seq, Bytes bytes) {
		IbTreeMutator<Bytes> mutator = ibTree.begin();
		mutator.put(key(keyUtil.hash(name), DATAID, seq), bytes);
		mutator.commit();
	}

	@Override
	public void resize(Bytes name, int size1) {
		IbTreeMutator<Bytes> mutator = ibTree.begin();
		Bytes hash = keyUtil.hash(name);
		Bytes sizeKey = key(hash, SIZEID, 0);
		int size0 = mutator.getData(sizeKey);
		int nPages0 = (size0 + pageSize - 1) / pageSize;
		int nPages1 = (size1 + pageSize - 1) / pageSize;

		for (int page = nPages1; page < nPages0; page++)
			mutator.remove(key(hash, DATAID, page));
		for (int page = nPages0; page < nPages1; page++)
			mutator.put(key(hash, DATAID, page), Bytes.emptyBytes);

		mutator.put(sizeKey, size1);
		mutator.commit();
	}

	private Bytes key(Bytes hash, int id, int seq) {
		return keyUtil.toDataKey(hash, id, seq).toBytes();
	}

}
