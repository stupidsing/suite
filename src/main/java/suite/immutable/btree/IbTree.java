package suite.immutable.btree;

import java.io.Closeable;

import suite.fs.KeyDataStoreMutator;

public interface IbTree<Key> extends Closeable {

	/**
	 * @return A new mutator object.
	 */
	public KeyDataStoreMutator<Key> begin();

	/**
	 * @return Calculate the maximum number of values that can be stored in this
	 *         tree before running out of pages, regardless of the branching
	 *         statuses, in a most conservative manner.
	 *
	 *         First, we relate the number of branches in nodes to the size of
	 *         the tree. For each branch node, it occupy 1 child of its parent,
	 *         and create children at the number of branch factor. Therefore its
	 *         "gain" is its branch factor minus 1. The tree root is a single
	 *         entry, thus the sum of all "gains" plus 1 result in the total
	 *         number of leave nodes.
	 *
	 *         Second, we find the smallest tree for n pages. 1 page is used as
	 *         the root which has 2 children at minimum. Other pages should have
	 *         half of branch factor at minimum.
	 *
	 *         Third, to cause page exhaustion at next insert, it require a
	 *         split to occur. Therefore 1 page should be at its maximum size.
	 *         This adds in half of branch factor minus 1 of nodes.
	 *
	 *         Fourth, the result needs to be minus by 1 to exclude the guard
	 *         node at rightmost of the tree.
	 *
	 *         Fifth, most mutators would acquire some new pages before old
	 *         pages could be discarded during commit. We have to reserve 10% of
	 *         pages for mutation use.
	 *
	 *         In formula, the minimum number of nodes causing split: 1 + (2 -
	 *         1) + (size - 1) * (minBranchFactor - 1) + (minBranchFactor - 1) -
	 *         1 = size * (minBranchFactor - 1) + 1
	 */
	public int guaranteedCapacity();

	public KeyDataStoreMutator<Key> create();

}
