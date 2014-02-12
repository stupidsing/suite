package suite.immutable;

import java.util.Comparator;

/**
 * Immutable skewed binomial priority queue, implemented using sparse-list of
 * trees.
 * 
 * @author ywsing
 */
public class SkewedBinomialPriorityQueue<T> {

	private Comparator<T> comparator;
	private ImmutableList<Node> trees;

	private class Node {
		private int rank;
		private T value;
		private ImmutableList<Node> nodes; // Note that rank(nodes.get(i)) = i

		private Node(T value) {
			this(0, value, ImmutableList.<Node> end());
		}

		private Node(int rank, T value, ImmutableList<Node> nodes) {
			this.rank = rank;
			this.nodes = nodes;
			this.value = value;
		}
	}

	private class FindMinimum {
		private Node tree;

		private T findMin() {
			T min = null;

			for (Node tree : trees) {
				T value = tree.value;
				if (min == null || comparator.compare(value, min) < 0) {
					this.tree = tree;
					min = value;
				}
			}

			return min;
		}

		private SkewedBinomialPriorityQueue<T> deleteMin() {
			findMin();

			ImmutableList<T> values0 = ImmutableList.<T> end();
			ImmutableList<Node> trees0 = ImmutableList.<Node> end();
			ImmutableList<Node> trees1 = ImmutableList.<Node> end();

			for (Node node : trees.reverse())
				if (node != tree)
					if (node.rank != 0)
						trees0 = ImmutableList.cons(node, trees0);
					else
						values0 = ImmutableList.cons(node.value, values0);

			for (Node node : tree.nodes.reverse())
				trees1 = ImmutableList.cons(node, trees1);

			trees1 = meld(trees0, trees1);

			for (T value : values0.reverse())
				trees1 = skewInsert(new Node(value), trees1);

			return new SkewedBinomialPriorityQueue<T>(comparator, trees1);
		}
	}

	public SkewedBinomialPriorityQueue(Comparator<T> comparator) {
		this(comparator, ImmutableList.<Node> end());
	}

	public SkewedBinomialPriorityQueue(Comparator<T> comparator, ImmutableList<Node> trees) {
		this.comparator = comparator;
		this.trees = trees;
	}

	public T findMin() {
		return new FindMinimum().findMin();
	}

	public int count() {
		return count(trees);
	}

	private int count(ImmutableList<Node> trees) {
		int c = 0;
		for (Node tree : trees)
			c += 1 + count(tree.nodes);
		return c;
	}

	public SkewedBinomialPriorityQueue<T> deleteMin() {
		return new FindMinimum().deleteMin();
	}

	public SkewedBinomialPriorityQueue<T> add(T value) {
		return new SkewedBinomialPriorityQueue<T>(comparator, skewInsert(new Node(value), trees));
	}

	public SkewedBinomialPriorityQueue<T> meld(SkewedBinomialPriorityQueue<T> pq) {
		return new SkewedBinomialPriorityQueue<>(comparator, meld(unskew(trees), unskew(pq.trees)));
	}

	private ImmutableList<Node> meld(ImmutableList<Node> trees0, ImmutableList<Node> trees1) {
		if (!trees0.isEmpty())
			if (!trees1.isEmpty()) {
				ImmutableList<Node> ts0, ts1;

				if (trees0.getHead().rank < trees1.getHead().rank) {
					ts0 = trees0;
					ts1 = trees1;
				} else {
					ts0 = trees1;
					ts1 = trees0;
				}

				Node head0 = ts0.getHead();
				Node head1 = ts1.getHead();
				ImmutableList<Node> tail0 = ts0.getTail();
				ImmutableList<Node> tail1 = ts1.getTail();

				if (head0.rank != head1.rank)
					return ImmutableList.cons(head0, meld(tail0, ImmutableList.cons(head1, tail1)));
				else
					return insert(link(head0, head1), meld(tail0, tail1));
			} else
				return trees0;
		else
			return trees1;
	}

	private ImmutableList<Node> unskew(ImmutableList<Node> trees) {
		return !trees.isEmpty() ? insert(trees.getHead(), trees.getTail()) : trees;
	}

	private ImmutableList<Node> skewInsert(Node node, ImmutableList<Node> trees) {
		ImmutableList<Node> tt, trees1;
		Node n0, n1;
		if (!trees.isEmpty() && !(tt = trees.getTail()).isEmpty() && (n0 = trees.getHead()).rank == (n1 = tt.getHead()).rank)
			trees1 = ImmutableList.cons(skewLink(node, n0, n1), tt.getTail());
		else
			trees1 = ImmutableList.cons(node, trees);
		return trees1;
	}

	private ImmutableList<Node> insert(Node tree, ImmutableList<Node> trees) {
		Node tree0;
		if (trees.isEmpty() || tree.rank < (tree0 = trees.getHead()).rank)
			return ImmutableList.cons(tree, trees);
		else
			return insert(link(tree, tree0), trees.getTail());
	}

	/**
	 * Links three nodes in skewed-binary fashion. Assumes node1 and node2
	 * having the same rank.
	 */
	private Node skewLink(Node node0, Node node1, Node node2) {
		int c01 = comparator.compare(node0.value, node1.value);
		int c12 = comparator.compare(node1.value, node2.value);
		int c20 = comparator.compare(node2.value, node0.value);
		ImmutableList<Node> nodes;
		Node smallest;

		if (c01 >= 0 && c12 <= 0) {
			smallest = node1;
			nodes = ImmutableList.cons(node0, ImmutableList.cons(node2, smallest.nodes));
		} else if (c12 >= 0 && c20 <= 0) {
			smallest = node2;
			nodes = ImmutableList.cons(node0, ImmutableList.cons(node1, smallest.nodes));
		} else { // c20 >= 0 && c01 <= 0
			smallest = node0;
			nodes = ImmutableList.asList(node1, node2);
		}

		return new Node(node1.rank + 1, smallest.value, nodes);
	}

	private Node link(Node node0, Node node1) {
		int rank = node0.rank;
		Node smaller, greater;

		if (comparator.compare(node0.value, node1.value) <= 0) {
			smaller = node0;
			greater = node1;
		} else {
			smaller = node1;
			greater = node0;
		}

		return new Node(rank + 1, smaller.value, ImmutableList.cons(greater, smaller.nodes));
	}

}
