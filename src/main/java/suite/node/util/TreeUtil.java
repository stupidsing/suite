package suite.node.util;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;

public class TreeUtil {

	public static int getNumberOfElements(Node node) {
		int n = 0;
		Tree tree;

		while ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
			node = tree.getRight();
			n++;
		}

		return n;
	}

	public static Node[] getElements(Node node, int n) {
		Node params[] = new Node[n];
		Tree tree;

		for (int i = 0; i < n; i++)
			if ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
				params[i] = tree.getLeft();
				node = tree.getRight();
			} else
				throw new RuntimeException("Not enough parameters");

		return params;
	}

	public static Node[] tuple(Node node) {
		Tree tree = Tree.decompose(node, TermOp.TUPLE_);
		return tree != null ? new Node[] { tree.getLeft(), tree.getRight() } : null;
	}

	public static boolean isList(Node node, Operator operator) {
		Tree tree;
		return node == Atom.NIL || (tree = Tree.decompose(node, operator)) != null && isList(tree.getRight(), operator);
	}

}
