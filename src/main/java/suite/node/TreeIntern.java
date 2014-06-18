package suite.node;

import java.util.HashMap;
import java.util.Map;

import suite.node.io.Operator;

/**
 * Tree that only have a single copy. Saves memory footprint.
 *
 * @author ywsing
 */
public class TreeIntern {

	private static Map<Key, Tree> interns = new HashMap<>();

	private static class Key {
		private int hashCode;
		private Operator operator;
		private Node left, right;

		public Key(Operator operator, Node left, Node right) {
			hashCode = 1;
			hashCode = 31 * hashCode + System.identityHashCode(left);
			hashCode = 31 * hashCode + System.identityHashCode(operator);
			hashCode = 31 * hashCode + System.identityHashCode(right);

			this.operator = operator;
			this.left = left;
			this.right = right;
		}

		public int hashCode() {
			return hashCode;
		}

		public boolean equals(Object object) {
			if (object.getClass() == Key.class) {
				Key key = (Key) object;
				return hashCode == key.hashCode && operator == key.operator && left == key.left && right == key.right;
			} else
				return false;
		}
	}

	public static Tree of(Operator operator, Node node0, Node node1) {
		Node left = node0.finalNode();
		Node right = node1.finalNode();
		return interns.computeIfAbsent(new Key(operator, left, right), any -> Tree.of(operator, left, right));
	}

}
