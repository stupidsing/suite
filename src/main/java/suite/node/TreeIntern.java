package suite.node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.node.io.Operator;
import suite.util.Object_;

/**
 * Tree that only have a single copy. Saves memory footprint.
 *
 * @author ywsing
 */
public class TreeIntern {

	private Map<Key, Node> interns = new ConcurrentHashMap<>();

	private static class Key {
		public int hashCode;

		public int hashCode() {
			return hashCode;
		}
	}

	private static class NodeKey extends Key {
		private Node node;

		public NodeKey(Node node) {
			hashCode = System.identityHashCode(node);
			this.node = node;
		}

		public boolean equals(Object object) {
			if (Object_.clazz(object) == NodeKey.class) {
				NodeKey key = (NodeKey) object;
				return hashCode == key.hashCode && node == key.node;
			} else
				return false;
		}
	}

	private static class TreeKey extends Key {
		private Operator operator;
		private Node left, right;

		public TreeKey(Operator operator, Node left, Node right) {
			int h = 7;
			h = h * 31 + System.identityHashCode(left);
			h = h * 31 + System.identityHashCode(operator);
			h = h * 31 + System.identityHashCode(right);

			hashCode = h;
			this.operator = operator;
			this.left = left;
			this.right = right;
		}

		public boolean equals(Object object) {
			if (Object_.clazz(object) == TreeKey.class) {
				TreeKey key = (TreeKey) object;
				return hashCode == key.hashCode && operator == key.operator && left == key.left && right == key.right;
			} else
				return false;
		}
	}

	public Node of(Operator operator, Node left, Node right) {
		return interns.computeIfAbsent(treeKey(operator, left, right), any -> Tree.of(operator, left, right));
	}

	public Node internalize(Node node) {
		Tree tree = Tree.decompose(node);
		Key key;
		if (tree != null)
			key = treeKey(tree.getOperator(), internalize(tree.getLeft()), internalize(tree.getRight()));
		else
			key = new NodeKey(node);
		return interns.computeIfAbsent(key, k -> node);
	}

	public void clear() {
		interns.clear();
	}

	private TreeKey treeKey(Operator operator, Node left, Node right) {
		return new TreeKey(operator, left, right);
	}

}
