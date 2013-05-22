package org.suite.node;

import java.util.HashMap;
import java.util.Map;

import org.parser.Operator;

/**
 * Tree that only have a single copy. Saves memory footprint.
 * 
 * @author ywsing
 */
public class TreeIntern {

	private static Map<Integer, Tree> interns = new HashMap<>();

	public static Tree create(Operator operator, Node left, Node right) {
		left = left.finalNode();
		right = right.finalNode();

		int hashCode = 1;
		hashCode = 31 * hashCode + System.identityHashCode(left);
		hashCode = 31 * hashCode + System.identityHashCode(operator);
		hashCode = 31 * hashCode + System.identityHashCode(right);

		Tree tree = interns.get(hashCode);
		if (tree == null)
			interns.put(hashCode, tree = Tree.create(operator, left, right));
		return tree;
	}

}
