package suite.node.util;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;

public class TreeUtil {

	public static Node[] elements(Node node0, int n) {
		Node[] params = new Node[n];
		Node node = node0;
		Tree tree;

		for (int i = 0; i < n - 1; i++)
			if ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
				params[i] = tree.getLeft();
				node = tree.getRight();
			} else
				throw new RuntimeException("Not enough parameters in " + node0);

		params[n - 1] = node;
		return params;
	}

	public static int nElements(Node node) {
		int n = 1;
		Tree tree;

		while ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
			node = tree.getRight();
			n++;
		}

		return n;
	}

	public static boolean isList(Node node, Operator operator) {
		Tree tree;
		return node == Atom.NIL || (tree = Tree.decompose(node, operator)) != null && isList(tree.getRight(), operator);
	}

}
