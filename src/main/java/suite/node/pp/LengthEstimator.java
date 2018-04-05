package suite.node.pp;

import java.util.HashMap;
import java.util.Map;

import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;

public class LengthEstimator {

	private Map<Integer, Integer> lengthByIds = new HashMap<>();
	private int defaultLength;

	public LengthEstimator(int defaultLength) {
		this.defaultLength = defaultLength;
	}

	public int estimateLengths(Node node) {
		var key = getKey(node);
		var length = lengthByIds.get(key);

		if (length == null) {
			int len;

			if (node instanceof Tree) {
				var tree = (Tree) node;

				var op = tree.getOperator();
				var len0 = estimateLengths(tree.getLeft());
				var len1 = estimateLengths(tree.getRight());
				var opLength = op.getName().length();

				len = len0 + len1 + opLength + 2; // rough estimation
			} else
				len = Formatter.dump(node).length();

			length = len;
			lengthByIds.put(key, length);
		}

		return length;
	}

	public int getEstimatedLength(Node node) {
		var length = lengthByIds.get(getKey(node));
		return length != null ? length : defaultLength; // maximum if not found
	}

	private int getKey(Node node) {
		return System.identityHashCode(node);
	}

}
