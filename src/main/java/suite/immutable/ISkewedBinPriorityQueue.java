package suite.immutable;

import java.util.Comparator;

/**
 * Immutable skewed binomial priority queue, implemented using sparse-list of
 * trees.
 *
 * @author ywsing
 */
public class ISkewedBinPriorityQueue<T> {

	private Comparator<T> comparator;
	private IList<Node> trees;

	private class Node {
		private int rank;
		private T value;
		private IList<Node> nodes; // note that rank(nodes.get(i)) = i

		private Node(T value) {
			this(0, value, IList.<Node> end());
		}

		private Node(int rank, T value, IList<Node> nodes) {
			this.rank = rank;
			this.nodes = nodes;
			this.value = value;
		}
	}

	private class FindMinimum {
		private Node tree;

		private T findMin() {
			T min = null;

			for (var tree : trees) {
				var value = tree.value;
				if (min == null || comparator.compare(value, min) < 0) {
					this.tree = tree;
					min = value;
				}
			}

			return min;
		}

		private ISkewedBinPriorityQueue<T> deleteMin() {
			findMin();

			var values0 = IList.<T> end();
			var trees0 = IList.<Node> end();
			var trees1 = IList.<Node> end();

			for (var node : trees.reverse())
				if (node != tree)
					if (node.rank != 0)
						trees0 = IList.cons(node, trees0);
					else
						values0 = IList.cons(node.value, values0);

			for (var node : tree.nodes.reverse())
				trees1 = IList.cons(node, trees1);

			trees1 = meld(trees0, trees1);

			for (var value : values0.reverse())
				trees1 = skewInsert(new Node(value), trees1);

			return new ISkewedBinPriorityQueue<>(comparator, trees1);
		}
	}

	public ISkewedBinPriorityQueue(Comparator<T> comparator) {
		this(comparator, IList.<Node> end());
	}

	public ISkewedBinPriorityQueue(Comparator<T> comparator, IList<Node> trees) {
		this.comparator = comparator;
		this.trees = trees;
	}

	public T findMin() {
		return new FindMinimum().findMin();
	}

	public int count() {
		return count(trees);
	}

	private int count(IList<Node> trees) {
		var c = 0;
		for (var tree : trees)
			c += 1 + count(tree.nodes);
		return c;
	}

	public ISkewedBinPriorityQueue<T> deleteMin() {
		return new FindMinimum().deleteMin();
	}

	public ISkewedBinPriorityQueue<T> add(T value) {
		return new ISkewedBinPriorityQueue<>(comparator, skewInsert(new Node(value), trees));
	}

	public ISkewedBinPriorityQueue<T> meld(ISkewedBinPriorityQueue<T> pq) {
		return new ISkewedBinPriorityQueue<>(comparator, meld(unskew(trees), unskew(pq.trees)));
	}

	private IList<Node> meld(IList<Node> trees0, IList<Node> trees1) {
		if (!trees0.isEmpty())
			if (!trees1.isEmpty()) {
				IList<Node> ts0, ts1;

				if (trees0.head.rank < trees1.head.rank) {
					ts0 = trees0;
					ts1 = trees1;
				} else {
					ts0 = trees1;
					ts1 = trees0;
				}

				var head0 = ts0.head;
				var head1 = ts1.head;
				var tail0 = ts0.tail;
				var tail1 = ts1.tail;

				if (head0.rank != head1.rank)
					return IList.cons(head0, meld(tail0, IList.cons(head1, tail1)));
				else
					return insert(link(head0, head1), meld(tail0, tail1));
			} else
				return trees0;
		else
			return trees1;
	}

	private IList<Node> unskew(IList<Node> trees) {
		return !trees.isEmpty() ? insert(trees.head, trees.tail) : trees;
	}

	private IList<Node> skewInsert(Node node, IList<Node> trees) {
		IList<Node> tt, trees1;
		Node n0, n1;
		if (!trees.isEmpty() && !(tt = trees.tail).isEmpty() && (n0 = trees.head).rank == (n1 = tt.head).rank)
			trees1 = IList.cons(skewLink(node, n0, n1), tt.tail);
		else
			trees1 = IList.cons(node, trees);
		return trees1;
	}

	private IList<Node> insert(Node tree, IList<Node> trees) {
		Node tree0;
		if (trees.isEmpty() || tree.rank < (tree0 = trees.head).rank)
			return IList.cons(tree, trees);
		else
			return insert(link(tree, tree0), trees.tail);
	}

	/**
	 * Links three nodes in skewed-binary fashion. Assumes node1 and node2
	 * having the same rank.
	 */
	private Node skewLink(Node node0, Node node1, Node node2) {
		int c01 = comparator.compare(node0.value, node1.value);
		int c12 = comparator.compare(node1.value, node2.value);
		int c20 = comparator.compare(node2.value, node0.value);
		IList<Node> nodes;
		Node smallest;

		if (0 <= c01 && c12 <= 0) {
			smallest = node1;
			nodes = IList.cons(node0, IList.cons(node2, smallest.nodes));
		} else if (0 <= c12 && c20 <= 0) {
			smallest = node2;
			nodes = IList.cons(node0, IList.cons(node1, smallest.nodes));
		} else { // 0 <= c20 && c01 <= 0
			smallest = node0;
			nodes = IList.of(node1, node2);
		}

		return new Node(node1.rank + 1, smallest.value, nodes);
	}

	private Node link(Node node0, Node node1) {
		var rank = node0.rank;
		Node smaller, greater;

		if (comparator.compare(node0.value, node1.value) <= 0) {
			smaller = node0;
			greater = node1;
		} else {
			smaller = node1;
			greater = node0;
		}

		return new Node(rank + 1, smaller.value, IList.cons(greater, smaller.nodes));
	}

}
