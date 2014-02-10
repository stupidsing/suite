package suite.immutable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Immutable binomial priority queue, implemented using dense-list of trees.
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

	private class FindMinimum {
		private Node tree;
		private int rank;

		private T findMin() {
			T min = null;
			Node node;

			for (int r = 0; r < maxRank; r++)
				if ((node = trees[r]) != null)
					if (min == null || comparator.compare(node.value, min) < 0) {
						tree = node;
						rank = r;
						min = node.value;
					}

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
			return pq0.meld(pq1);
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
		return meld(new BinomialPriorityQueue<>(comparator, forest));
	}

	public BinomialPriorityQueue<T> meld(BinomialPriorityQueue<T> pq) {
		Node forest[] = createForest();
		Node tree = null;

		for (int rank = 0; rank < maxRank; rank++) {
			List<Node> list1 = new ArrayList<>();

			for (Node t : Arrays.asList(tree, trees[rank], pq.trees[rank]))
				if (t != null)
					list1.add(t);

			int size = list1.size();

			if (size >= 2) {
				tree = link(rank, list1.get(0), list1.get(1));
				list1 = list1.subList(2, size);
			} else
				tree = null;

			forest[rank] = !list1.isEmpty() ? list1.get(0) : null;
		}

		return new BinomialPriorityQueue<>(comparator, forest);
	}

	private Node link(int rank, Node node0, Node node1) {
		Node smaller, greater;

		if (comparator.compare(node0.value, node1.value) <= 0) {
			smaller = node0;
			greater = node1;
		} else {
			smaller = node1;
			greater = node0;
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
