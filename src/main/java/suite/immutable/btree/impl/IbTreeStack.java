package suite.immutable.btree.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import suite.immutable.btree.IbTree;
import suite.util.Util;

public class IbTreeStack<Key> implements Closeable {

	private List<IbTreeImpl<Integer>> allocationIbTrees = new ArrayList<>();
	private IbTree<Key> ibTree;

	public IbTreeStack(IbTreeConfiguration<Key> config) {
		String filenamePrefix = config.getFilenamePrefix();
		int pageSize = config.getPageSize();
		long capacity = config.getCapacity();
		long nPages = capacity / pageSize;

		IbTreeBuilder builder = new IbTreeBuilder(config);

		int i = 0;
		IbTreeImpl<Integer> allocationIbTree;
		allocationIbTrees.add(builder.buildAllocationIbTree(filenamePrefix + i++));

		while ((allocationIbTree = Util.last(allocationIbTrees)).guaranteedCapacity() < nPages)
			allocationIbTrees.add(builder.buildAllocationIbTree(filenamePrefix + i++, allocationIbTree));

		ibTree = builder.buildTree(filenamePrefix + i++, config, allocationIbTree);
	}

	@Override
	public void close() throws IOException {
		ibTree.close();
		ListIterator<IbTreeImpl<Integer>> li = allocationIbTrees.listIterator();
		while (li.hasPrevious())
			li.previous().close();
	}

	public IbTree<Key> getIbTree() {
		return ibTree;
	}

}
