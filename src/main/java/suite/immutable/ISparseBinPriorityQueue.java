package suite.immutable;

import java.util.Comparator;
import java.util.Deque;

/**
 * Immutable binomial priority queue, implemented using sparse-list of trees.
 *
 * @author ywsing
 */
public class ISparseBinPriorityQueue<T> {

	private Comparator<T> comparator;
	private IList<Tree> trees;

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
		private IList<Node> nodes; // note that rank(nodes.get(i)) = i

		private Node(T value) {
			this(value, IList.<Node> end());
		}

		private Node(T value, IList<Node> nodes) {
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

		private ISparseBinPriorityQueue<T> deleteMin() {
			findMin();

			var trees0 = IList.<Tree> end();
			var trees1 = IList.<Tree> end();

			for (var t : trees.reverse())
				if (t.rank != tree.rank)
					trees0 = IList.cons(t, trees0);

			var nr = tree.root.nodes.reverse();
			var rank = nr.size();

			for (Node node_ : nr)
				trees1 = IList.cons(new Tree(--rank, node_), trees1);

			return new ISparseBinPriorityQueue<>(comparator, meld(trees0, trees1));
		}
	}

	public ISparseBinPriorityQueue(Comparator<T> comparator) {
		this(comparator, IList.<Tree> end());
	}

	public ISparseBinPriorityQueue(Comparator<T> comparator, IList<Tree> trees) {
		this.comparator = comparator;
		this.trees = trees;
	}

	public T findMin() {
		return new FindMinimum().findMin();
	}

	public ISparseBinPriorityQueue<T> deleteMin() {
		return new FindMinimum().deleteMin();
	}

	public ISparseBinPriorityQueue<T> add(T value) {
		return new ISparseBinPriorityQueue<>(comparator, insert(new Tree(0, new Node(value)), trees));
	}

	public ISparseBinPriorityQueue<T> meld(ISparseBinPriorityQueue<T> pq) {
		return new ISparseBinPriorityQueue<>(comparator, meld(trees, pq.trees));
	}

	private IList<Tree> meld(IList<Tree> trees0, IList<Tree> trees1) {
		if (!trees0.isEmpty())
			if (!trees1.isEmpty()) {
				IList<Tree> ts0, ts1;

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

	private IList<Tree> insert(Tree tree, IList<Tree> trees) {
		Tree tree0;
		if (trees.isEmpty() || tree.rank < (tree0 = trees.head).rank)
			return IList.cons(tree, trees);
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

		return new Tree(rank + 1, new Node(smaller.root.value, IList.cons(greater.root, smaller.root.nodes)));
	}

}
