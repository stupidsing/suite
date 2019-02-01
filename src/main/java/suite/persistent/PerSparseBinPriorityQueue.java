package suite.persistent;

import java.util.Comparator;

/**
 * Persistent binomial priority queue, implemented using sparse-list of trees.
 *
 * @author ywsing
 */
public class PerSparseBinPriorityQueue<T> {

	private Comparator<T> comparator;
	private PerList<Tree> trees;

	private class Tree {
		private int rank;
		private Node root;

		private Tree(Node root) {
			this(0, root);
		}

		private Tree(int rank, Node root) {
			this.rank = rank;
			this.root = root;
		}
	}

	private class Node {
		private T value;
		private PerList<Node> nodes; // note that rank(nodes.get(i)) = i

		private Node(T value) {
			this(value, PerList.<Node> end());
		}

		private Node(T value, PerList<Node> nodes) {
			this.nodes = nodes;
			this.value = value;
		}
	}

	private class FindMinimum {
		private Tree tree;

		private T findMin() {
			T min = null;

			for (var tree : trees) {
				var value = tree.root.value;
				if (min == null || comparator.compare(value, min) < 0) {
					this.tree = tree;
					min = value;
				}
			}

			return min;
		}

		private PerSparseBinPriorityQueue<T> deleteMin() {
			findMin();

			var trees0 = PerList.<Tree> end();
			var trees1 = PerList.<Tree> end();

			for (var t : trees.reverse())
				if (t.rank != tree.rank)
					trees0 = PerList.cons(t, trees0);

			var nr = tree.root.nodes.reverse();
			var rank = nr.size();

			for (var node_ : nr)
				trees1 = PerList.cons(new Tree(--rank, node_), trees1);

			return new PerSparseBinPriorityQueue<>(comparator, meld(trees0, trees1));
		}
	}

	public PerSparseBinPriorityQueue(Comparator<T> comparator) {
		this(comparator, PerList.<Tree> end());
	}

	public PerSparseBinPriorityQueue(Comparator<T> comparator, PerList<Tree> trees) {
		this.comparator = comparator;
		this.trees = trees;
	}

	public T findMin() {
		return new FindMinimum().findMin();
	}

	public PerSparseBinPriorityQueue<T> deleteMin() {
		return new FindMinimum().deleteMin();
	}

	public PerSparseBinPriorityQueue<T> add(T value) {
		return new PerSparseBinPriorityQueue<>(comparator, insert(new Tree(0, new Node(value)), trees));
	}

	public PerSparseBinPriorityQueue<T> meld(PerSparseBinPriorityQueue<T> pq) {
		return new PerSparseBinPriorityQueue<>(comparator, meld(trees, pq.trees));
	}

	private PerList<Tree> meld(PerList<Tree> trees0, PerList<Tree> trees1) {
		if (!trees0.isEmpty())
			if (!trees1.isEmpty()) {
				PerList<Tree> ts0, ts1;

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
					return PerList.cons(head0, meld(tail0, PerList.cons(head1, tail1)));
				else
					return insert(link(head0, head1), meld(tail0, tail1));
			} else
				return trees0;
		else
			return trees1;
	}

	private PerList<Tree> insert(Tree tree, PerList<Tree> trees) {
		Tree tree0;
		if (trees.isEmpty() || tree.rank < (tree0 = trees.head).rank)
			return PerList.cons(tree, trees);
		else
			return insert(link(tree, tree0), trees.tail);
	}

	private Tree link(Tree tree0, Tree tree1) {
		var rank = tree0.rank;
		Tree smaller, greater;

		if (comparator.compare(tree0.root.value, tree1.root.value) <= 0) {
			smaller = tree0;
			greater = tree1;
		} else {
			smaller = tree1;
			greater = tree0;
		}

		return new Tree(rank + 1, new Node(smaller.root.value, PerList.cons(greater.root, smaller.root.nodes)));
	}

}
