package suite.lcs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import suite.search.Search;
import suite.search.Search.Traverser;
import suite.util.Object_;

/**
 * Longest common subsequence using breadth-first search.
 *
 * @author ywsing
 */
public class LcsBfs<T> {

	private class Node {
		private Node previous;
		private int pos0;
		private int pos1;

		private Node(Node previous, int pos0, int pos1) {
			this.previous = previous;
			this.pos0 = pos0;
			this.pos1 = pos1;
		}

		public boolean equals(Object object) {
			if (Object_.clazz(object) == Node.class) {
				LcsBfs<?>.Node node = (LcsBfs<?>.Node) object;
				return pos0 == node.pos0 && pos1 == node.pos1;
			} else
				return false;
		}

		public int hashCode() {
			var j = 7;
			j = 31 * j + pos0;
			j = 31 * j + pos1;
			return j;
		}
	}

	public List<T> lcs(List<T> l0, List<T> l1) {
		var size0 = l0.size();
		var size1 = l1.size();

		var node = Search.breadthFirst(new Traverser<>() {
			public List<Node> generate(Node node) {
				List<Node> nodes = new ArrayList<>();
				if (node.pos0 < size0)
					nodes.add(jump(new Node(node, node.pos0 + 1, node.pos1)));
				if (node.pos1 < size1)
					nodes.add(jump(new Node(node, node.pos0, node.pos1 + 1)));
				return nodes;
			}

			public boolean evaluate(Node node) {
				return node.pos0 == size0 && node.pos1 == size1;
			}

			private Node jump(Node node) {
				while (node.pos0 < size0 //
						&& node.pos1 < size1 //
						&& Objects.equals(l0.get(node.pos0), l1.get(node.pos1))) {
					node.pos0++;
					node.pos1++;
				}

				return node;
			}
		}, new Node(null, 0, 0));

		Deque<T> deque = new ArrayDeque<>();
		Node previous;

		while ((previous = node.previous) != null) {
			var pos0 = node.pos0;
			var pos1 = node.pos1;

			while (previous.pos0 < pos0 && previous.pos1 < pos1) {
				pos0--;
				pos1--;
				deque.addFirst(l0.get(pos0));
			}

			node = previous;
		}

		return new ArrayList<>(deque);
	}

}
