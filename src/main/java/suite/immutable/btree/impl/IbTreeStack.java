package suite.immutable.btree.impl;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import suite.immutable.btree.IbTree;
import suite.util.SerializeUtil.Serializer;
import suite.util.Util;

public class IbTreeStack<Key> implements Closeable {

	private List<IbTreeImpl<Integer>> pointerIbTrees = new ArrayList<>();
	private IbTree<Key> ibTree;

	public IbTreeStack(String filename, long capacity, int pageSize, Comparator<Key> comparator, Serializer<Key> serializer)
			throws FileNotFoundException {
		long nPages = capacity / pageSize;
		IbTreeBuilder builder = new IbTreeBuilder(pageSize / 64, pageSize);

		int i = 0;
		IbTreeImpl<Integer> pointerIbTree;
		pointerIbTrees.add(builder.buildPointerTree(filename + i++));

		while ((pointerIbTree = Util.last(pointerIbTrees)).guaranteedCapacity() < nPages)
			pointerIbTrees.add(builder.buildPointerTree(filename + i++, pointerIbTree));

		ibTree = builder.buildTree(filename + i++, comparator, serializer, pointerIbTree);
	}

	@Override
	public void close() {
		ibTree.close();
		ListIterator<IbTreeImpl<Integer>> li = pointerIbTrees.listIterator();
		while (li.hasPrevious())
			li.previous().close();
	}

	public IbTree<Key> getIbTree() {
		return ibTree;
	}

}
