package org.suite.predicates;

import java.util.ArrayList;
import java.util.List;

import org.suite.doer.TermParser.TermOp;
import org.suite.node.Node;
import org.suite.node.Tree;

public class Predicate {

	// (a, b, c, d) becomes { a, b, c, d } in Java
	public static Node[] getParameters(Node node, int n) {
		List<Node> results = new ArrayList<>(n);
		Tree tree;

		for (int i = 1; i < n; i++)
			if ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
				results.add(tree.getLeft());
				node = tree.getRight();
			} else
				throw new RuntimeException("Not enough parameters");

		results.add(node);
		return results.toArray(new Node[results.size()]);
	}

}
