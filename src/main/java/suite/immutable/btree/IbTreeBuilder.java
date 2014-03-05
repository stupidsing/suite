package suite.immutable.btree;

import java.io.FileNotFoundException;
import java.util.Comparator;

import suite.util.SerializeUtil.Serializer;
import suite.util.Util;

public class IbTreeBuilder {

	private int maxBranchFactor;
	private int pageSize;

	public IbTreeBuilder(int maxBranchFactor, int pageSize) {
		this.maxBranchFactor = maxBranchFactor;
		this.pageSize = pageSize;
	}

	/**
	 * Builds a small tree that would not span more than 1 page, i.e. no extra
	 * "page allocation tree" is required.
	 */
	public IbTree<Integer> buildPointerTree(String filename) throws FileNotFoundException {
		return buildPointerTree(filename, null);
	}

	/**
	 * Builds an intermediate tree that is supported by a separate page
	 * allocation tree.
	 */
	public IbTree<Integer> buildPointerTree(String filename, IbTree<Integer> allocationIbTree) throws FileNotFoundException {
		return buildTree(filename, Util.<Integer> comparator(), IbTree.pointerSerializer, allocationIbTree);
	}

	public <Key> IbTree<Key> buildTree(String filename //
			, Comparator<Key> comparator //
			, Serializer<Key> serializer //
			, IbTree<Integer> allocationIbTree) throws FileNotFoundException {
		return new IbTree<>(filename, maxBranchFactor, pageSize, comparator, serializer, allocationIbTree);
	}

}
