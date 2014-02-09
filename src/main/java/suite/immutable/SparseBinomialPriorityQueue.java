package suite.immutable;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;

/**
 * Immutable binomial priority queue. A sparse list implementation.
 * 
 * @author ywsing
 */
public class SparseBinomialPriorityQueue<T> {

	private Comparator<T> comparator;
	private ImmutableList<Tree> trees;

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
		private ImmutableList<Node> nodes; // Note that rank(nodes.get(i)) = i

		private Node(T value) {
			this(value, ImmutableList.<Node> end());
		}

		private Node(T value, ImmutableList<Node> nodes) {
			this.nodes = nodes;
			this.value = value;
		}
	}

	private class FindMinimum {
		private Tree tree;

		private T findMin() {
			T min = null;

			for (Tree tree : trees) {
				T value = tree.root.value;
				if (min == null || comparator.compare(value, min) < 0) {
					this.tree = tree;
					min = value;
				}
			}

			return min;
		}

		private SparseBinomialPriorityQueue<T> deleteMin() {
			findMin();

			ImmutableList<Tree> trees0 = ImmutableList.<Tree> end();
			ImmutableList<Tree> trees1 = ImmutableList.<Tree> end();

			for (Tree t : reverse(trees))
				if (t.rank != tree.rank)
					trees0 = ImmutableList.cons(t, trees0);

			Deque<Node> nr = reverse(tree.root.nodes);
			int rank = nr.size();

			for (Node node_ : nr)
				trees1 = ImmutableList.cons(new Tree(--rank, node_), trees1);

			return new SparseBinomialPriorityQueue<T>(comparator, meld(trees0, trees1));
		}

		private <U> Deque<U> reverse(ImmutableList<U> list) {
			Deque<U> deque = new ArrayDeque<>();
			for (U u : list)
				deque.addFirst(u);
			return deque;
		}
	}

	public SparseBinomialPriorityQueue(Comparator<T> comparator) {
		this(comparator, ImmutableList.<Tree> end());
	}

	public SparseBinomialPriorityQueue(Comparator<T> comparator, ImmutableList<Tree> trees) {
		this.comparator = comparator;
		this.trees = trees;
	}

	public T findMin() {
		return new FindMinimum().findMin();
	}

	public SparseBinomialPriorityQueue<T> deleteMin() {
		return new FindMinimum().deleteMin();
	}

	public SparseBinomialPriorityQueue<T> add(T value) {
		return new SparseBinomialPriorityQueue<T>(comparator, insert(new Tree(0, new Node(value)), trees));
	}

	public SparseBinomialPriorityQueue<T> meld(SparseBinomialPriorityQueue<T> pq) {
		return new SparseBinomialPriorityQueue<>(comparator, meld(trees, pq.trees));
	}

	private ImmutableList<Tree> meld(ImmutableList<Tree> trees0, ImmutableList<Tree> trees1) {
		if (!trees0.isEmpty())
			if (!trees1.isEmpty()) {
				ImmutableList<Tree> ts0, ts1;

				if (trees0.getHead().rank < trees1.getHead().rank) {
					ts0 = trees0;
					ts1 = trees1;
				} else {
					ts0 = trees1;
					ts1 = trees0;
				}

				Tree head0 = ts0.getHead();
				Tree head1 = ts1.getHead();
				ImmutableList<Tree> tail0 = ts0.getTail();
				ImmutableList<Tree> tail1 = ts1.getTail();

				if (head0.rank != head1.rank)
					return ImmutableList.cons(head0, meld(tail0, ImmutableList.cons(head1, tail1)));
				else
					return insert(link(head0, head1), meld(tail0, tail1));
			} else
				return trees0;
		else
			return trees1;
	}

	private ImmutableList<Tree> insert(Tree tree, ImmutableList<Tree> trees) {
		Tree tree0;
		if (trees.isEmpty() || tree.rank < (tree0 = trees.getHead()).rank)
			return ImmutableList.cons(tree, trees);
		else
			return insert(link(tree, tree0), trees.getTail());
	}

	private Tree link(Tree tree0, Tree tree1) {
		int rank = tree0.rank;
		Tree smaller, greater;

		if (comparator.compare(tree0.root.value, tree1.root.value) <= 0) {
			smaller = tree0;
			greater = tree1;
		} else {
			smaller = tree1;
			greater = tree0;
		}

		return new Tree(rank + 1, new Node(smaller.root.value, ImmutableList.cons(greater.root, smaller.root.nodes)));
	}

}
