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

	private Map<Key, Tree> interns = new ConcurrentHashMap<>();

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

		public boolean equals(Object object) {
			if (Object_.clazz(object) == Key.class) {
				Key key = (Key) object;
				return hashCode == key.hashCode && operator == key.operator && left == key.left && right == key.right;
			} else
				return false;
		}

		public int hashCode() {
			return hashCode;
		}
	}

	public Tree of(Operator operator, Node left, Node right) {
		return interns.computeIfAbsent(new Key(operator, left, right), any -> Tree.of(operator, left, right));
	}

	public void clear() {
		interns.clear();
	}

}
