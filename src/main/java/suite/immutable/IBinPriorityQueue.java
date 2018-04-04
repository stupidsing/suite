package suite.immutable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import suite.util.Array_;

/**
 * Immutable binomial priority queue, implemented using dense-list of trees.
 *
 * @author ywsing
 */
public class IBinPriorityQueue<T> {

	private static int maxRank = 64;

	private Comparator<T> comparator;

	/**
	 * Forest of trees.
	 *
	 * Note that trees[rank].nodes.size = rank if trees[rank] is not null.
	 */
	private Node[] trees;

	private class Node {
		private List<Node> nodes; // note that rank(nodes.get(i)) = i
		private T value;

		public Node(T value) {
			this(new ArrayList<>(), value);
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

		private IBinPriorityQueue<T> deleteMin() {
			findMin();

			Node[] forest0 = newForest();
			Node[] forest1 = newForest();

			for (int rank = 0; rank < maxRank; rank++)
				forest0[rank] = trees[rank];
			forest0[rank] = null;

			var rank = 0;

			for (Node node : tree.nodes)
				forest1[rank++] = node;

			IBinPriorityQueue<T> pq0 = new IBinPriorityQueue<>(comparator, forest0);
			IBinPriorityQueue<T> pq1 = new IBinPriorityQueue<>(comparator, forest1);
			return pq0.meld(pq1);
		}
	}

	@SuppressWarnings("unchecked")
	public IBinPriorityQueue(Comparator<T> comparator) {
		this(comparator, Array_.newArray(Node.class, maxRank));
	}

	public IBinPriorityQueue(Comparator<T> comparator, Node[] trees) {
		this.comparator = comparator;
		this.trees = trees;
	}

	public T findMin() {
		return new FindMinimum().findMin();
	}

	public IBinPriorityQueue<T> deleteMin() {
		return new FindMinimum().deleteMin();
	}

	public IBinPriorityQueue<T> add(T value) {
		Node[] forest = newForest();
		forest[0] = new Node(value);
		return meld(new IBinPriorityQueue<>(comparator, forest));
	}

	public IBinPriorityQueue<T> meld(IBinPriorityQueue<T> pq) {
		Node[] forest = newForest();
		Node tree = null;

		for (int rank = 0; rank < maxRank; rank++) {
			List<Node> list1 = new ArrayList<>();

			for (Node t : List.of(tree, trees[rank], pq.trees[rank]))
				if (t != null)
					list1.add(t);

			var size = list1.size();

			if (2 <= size) {
				tree = link(rank, list1.get(0), list1.get(1));
				list1 = list1.subList(2, size);
			} else
				tree = null;

			forest[rank] = !list1.isEmpty() ? list1.get(0) : null;
		}

		return new IBinPriorityQueue<>(comparator, forest);
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

	private Node[] newForest() {
		@SuppressWarnings("unchecked")
		Node[] forest = Array_.newArray(Node.class, maxRank);
		return forest;
	}

}
