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

	private static Map<Integer, Tree> interns = new HashMap<>();

	public static Tree create(Operator operator, Node node0, Node node1) {
		Node left = node0.finalNode();
		Node right = node1.finalNode();

		int hashCode = 1;
		hashCode = 31 * hashCode + System.identityHashCode(left);
		hashCode = 31 * hashCode + System.identityHashCode(operator);
		hashCode = 31 * hashCode + System.identityHashCode(right);

		return interns.computeIfAbsent(hashCode, any -> Tree.create(operator, left, right));
	}

}
