package suite.immutable.btree.impl;

import java.io.FileNotFoundException;

import suite.util.Util;

public class IbTreeBuilder {

	private IbTreeConfiguration<Integer> allocationIbTreeConfig;

	public IbTreeBuilder(IbTreeConfiguration<?> config) {
		allocationIbTreeConfig = new IbTreeConfiguration<Integer>();
		allocationIbTreeConfig.setPageSize(config.getPageSize());
		allocationIbTreeConfig.setMaxBranchFactor(config.getMaxBranchFactor());
		allocationIbTreeConfig.setComparator(Util.<Integer> comparator());
		allocationIbTreeConfig.setSerializer(IbTreeImpl.pointerSerializer);
	}

	/**
	 * Builds a small tree that would not span more than 1 page, i.e. no extra
	 * "page allocation tree" is required.
	 */
	public IbTreeImpl<Integer> buildAllocationIbTree(String filename) throws FileNotFoundException {
		return buildAllocationIbTree(filename, null);
	}

	/**
	 * Builds an intermediate tree that is supported by a separate page
	 * allocation tree.
	 */
	public IbTreeImpl<Integer> buildAllocationIbTree(String filename, IbTreeImpl<Integer> allocationIbTree)
			throws FileNotFoundException {
		return buildTree(filename, allocationIbTreeConfig, allocationIbTree);
	}

	public <Key> IbTreeImpl<Key> buildTree(String filename, IbTreeConfiguration<Key> config, IbTreeImpl<Integer> allocationIbTree)
			throws FileNotFoundException {
		return new IbTreeImpl<Key>(filename, config, allocationIbTree);
	}

}
