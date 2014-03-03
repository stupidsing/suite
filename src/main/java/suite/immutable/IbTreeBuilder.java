package suite.immutable;

import java.io.FileNotFoundException;
import java.util.Comparator;

import suite.immutable.IbTree.Pointer;
import suite.util.SerializeUtil.Serializer;

public class IbTreeBuilder {

	private int maxBranchFactor;

	public IbTreeBuilder(int maxBranchFactor) {
		this.maxBranchFactor = maxBranchFactor;
	}

	/**
	 * Builds a small tree that would not span more than 1 page, i.e. no extra
	 * "page allocation tree" is required.
	 */
	public IbTree<Pointer> buildPointerTree(String filename) throws FileNotFoundException {
		return buildPointerTree(filename, null);
	}

	/**
	 * Builds an intermediate tree that is supported by a separate page
	 * allocation tree.
	 */
	public IbTree<Pointer> buildPointerTree(String filename, IbTree<Pointer> allocationIbTree) throws FileNotFoundException {
		return new IbTree<Pointer>(filename, maxBranchFactor, Pointer.comparator, Pointer.serializer, allocationIbTree);
	}

	public <Key> IbTree<Key> buildTree(String filename //
			, Comparator<Key> comparator //
			, Serializer<Key> serializer //
			, IbTree<Pointer> allocationIbTree) throws FileNotFoundException {
		return new IbTree<>(filename, maxBranchFactor, comparator, serializer, allocationIbTree);
	}

}
