package suite.node.pp;

import java.util.HashMap;
import java.util.Map;

import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;

/**
 * Estimate the length in characters when displaying a node as string.
 *
 * @author ywsing
 */
public class EstimateLength {

	private Map<Integer, Integer> lengthByIds;
	private int defaultLength;

	public EstimateLength(int defaultLength, Node node) {
		this.defaultLength = defaultLength;
		lengthByIds = new HashMap<>();

		new Object() {
			private int estimate(Node node) {
				var key = getKey(node);
				var length = lengthByIds.get(key);

				if (length == null) {
					int len;

					if (node instanceof Tree) {
						var tree = (Tree) node;

						var op = tree.getOperator();
						var len0 = estimate(tree.getLeft());
						var len1 = estimate(tree.getRight());
						var opLength = op.name_().length();

						len = len0 + len1 + opLength + 2; // rough estimation
					} else
						len = Formatter.dump(node).length();

					length = len;
					lengthByIds.put(key, length);
				}

				return length;
			}
		}.estimate(node);
	}

	public int getEstimatedLength(Node node) {
		var length = lengthByIds.get(getKey(node));
		return length != null ? length : defaultLength; // maximum if not found
	}

	private int getKey(Node node) {
		return System.identityHashCode(node);
	}

}
