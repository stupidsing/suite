package suite.immutable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Immutable binomial priority queue.
 * 
 * @author ywsing
 */
public class BinomialPriorityQueue<T> {

	private static final int maxRank = 64;

	private Comparator<T> comparator;

	/**
	 * Forest of trees.
	 * 
	 * Note that trees[rank].nodes.size = rank if trees[rank] is not null.
	 */
	private Node trees[];

	private class FindMinimum {
		private Node tree;
		private int rank;

		private T findMin() {
			T min = null;

			for (rank = 0; rank < maxRank; rank++)
				if ((tree = trees[rank]) != null)
					if (min == null || comparator.compare(tree.value, min) < 0)
						min = tree.value;

			return min;
		}

		private BinomialPriorityQueue<T> deleteMin() {
			findMin();

			Node forest0[] = createForest();
			Node forest1[] = createForest();

			for (int rank = 0; rank < maxRank; rank++)
				forest0[rank] = trees[rank];
			forest0[rank] = null;

			int rank = 0;

			for (Node node : tree.nodes)
				forest1[rank++] = node;

			BinomialPriorityQueue<T> pq0 = new BinomialPriorityQueue<>(comparator, forest0);
			BinomialPriorityQueue<T> pq1 = new BinomialPriorityQueue<>(comparator, forest1);
			return pq0.merge(pq1);
		}
	}

	private class Node {
		private List<Node> nodes; // Note that rank(nodes.get(i)) = i

		private T value;

		public Node(T value) {
			this(new ArrayList<Node>(), value);
		}

		public Node(List<Node> nodes, T value) {
			this.nodes = nodes;
			this.value = value;
		}
	}

	@SuppressWarnings("unchecked")
	public BinomialPriorityQueue(Comparator<T> comparator) {
		this(comparator, (BinomialPriorityQueue<T>.Node[]) new BinomialPriorityQueue<?>.Node[maxRank]);
	}

	public BinomialPriorityQueue(Comparator<T> comparator, Node trees[]) {
		this.comparator = comparator;
		this.trees = trees;
	}

	public T findMin() {
		return new FindMinimum().findMin();
	}

	public BinomialPriorityQueue<T> deleteMin() {
		return new FindMinimum().deleteMin();
	}

	public BinomialPriorityQueue<T> add(T value) {
		Node forest[] = createForest();
		forest[0] = new Node(value);
		return merge(new BinomialPriorityQueue<>(comparator, forest));
	}

	public BinomialPriorityQueue<T> merge(BinomialPriorityQueue<T> pq) {
		Node forest[] = createForest();
		Node tree = null;

		for (int rank = 0; rank < maxRank; rank++) {
			List<Node> list1 = new ArrayList<>();

			for (Node t : Arrays.asList(tree, trees[rank], pq.trees[rank]))
				if (t != null)
					list1.add(t);

			int size = list1.size();

			if (size > 2) {
				tree = mergeTrees(rank, list1.get(0), list1.get(1));
				list1 = list1.subList(2, size);
			} else
				tree = null;

			forest[rank] = !list1.isEmpty() ? list1.get(0) : null;
		}

		return new BinomialPriorityQueue<>(comparator, forest);
	}

	private Node mergeTrees(int rank, Node tree0, Node tree1) {
		Node smaller, greater;

		if (comparator.compare(tree0.value, tree1.value) <= 0) {
			smaller = tree0;
			greater = tree1;
		} else {
			smaller = tree1;
			greater = tree0;
		}

		List<Node> nodes = new ArrayList<>(rank);
		nodes.addAll(smaller.nodes);
		nodes.add(greater);

		return new Node(nodes, smaller.value);
	}

	private Node[] createForest() {
		@SuppressWarnings("unchecked")
		Node forest[] = (BinomialPriorityQueue<T>.Node[]) new BinomialPriorityQueue<?>.Node[maxRank];
		return forest;
	}

}
