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

	private Map<TreeKey, Tree> interns = new ConcurrentHashMap<>();

	private static class TreeKey {
		private int hashCode;
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

		public int hashCode() {
			return hashCode;
		}
	}

	public Tree of(Operator operator, Node left, Node right) {
		return interns.computeIfAbsent(new TreeKey(operator, left, right), any -> Tree.of(operator, left, right));
	}

	public void clear() {
		interns.clear();
	}

}
