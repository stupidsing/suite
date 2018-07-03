package suite.node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.node.io.Operator;
import suite.node.io.SwitchNode;
import suite.object.Object_;

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
				var key = (NodeKey) object;
				return hashCode == key.hashCode && node == key.node;
			} else
				return false;
		}
	}

	private static class TreeKey extends Key {
		private Operator operator;
		private Node left, right;

		public TreeKey(Operator operator, Node left, Node right) {
			var h = 7;
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
				var key = (TreeKey) object;
				return hashCode == key.hashCode && operator == key.operator && left == key.left && right == key.right;
			} else
				return false;
		}
	}

	public Node of(Operator operator, Node left, Node right) {
		return interns.computeIfAbsent(treeKey(operator, left, right), any -> Tree.of(operator, left, right));
	}

	public Node intern(Node node) {
		var key = new SwitchNode<Key>(node) //
				.applyTree((op, l, r) -> treeKey(op, intern(l), intern(r))) //
				.applyIf(Node.class, NodeKey::new) //
				.result();
		return interns.computeIfAbsent(key, k -> node);
	}

	public void clear() {
		interns.clear();
	}

	private TreeKey treeKey(Operator operator, Node left, Node right) {
		return new TreeKey(operator, left, right);
	}

}
