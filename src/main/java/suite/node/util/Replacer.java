package suite.node.util;

import suite.node.Node;
import suite.node.Tree;

public class Replacer {

	public static boolean contains(Node node, Node key) {
		boolean result;

		if (!node.equals(key)) {
			Tree tree = Tree.decompose(node);

			if (tree != null)
				result = contains(tree.getLeft(), key) || contains(tree.getRight(), key);
			else
				result = false;
		} else
			result = true;

		return result;
	}

	public static Node replace(Node node0, Node from, Node to) {
		Node node1;

		if (!node0.equals(from)) {
			Tree tree = Tree.decompose(node0);

			if (tree != null)
				node1 = Tree.create(tree.getOperator() //
						, replace(tree.getLeft(), from, to) //
						, replace(tree.getRight(), from, to));
			else
				node1 = node0;
		} else
			node1 = to;

		return node1;
	}

}
