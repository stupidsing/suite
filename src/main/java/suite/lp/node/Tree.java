package suite.lp.node;

import suite.parser.Operator;
import suite.util.Util;

public class Tree extends Node {

	private Operator operator;
	private Node left, right;

	private Tree(Operator operator, Node left, Node right) {
		this.operator = operator;
		this.left = left;
		this.right = right;
	}

	public static Tree create(Operator operator, Node left, Node right) {
		return new Tree(operator, left, right);
	}

	public static Tree decompose(Node node) {
		node = node.finalNode();
		return node instanceof Tree ? (Tree) node : null;
	}

	public static Tree decompose(Node node, Operator operator) {
		node = node.finalNode();
		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			if (tree.operator == operator)
				return tree;
		}
		return null;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + Util.hashCode(left);
		result = 31 * result + Util.hashCode(operator);
		result = 31 * result + Util.hashCode(right);
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Node) {
			Node node = ((Node) object).finalNode();
			if (node instanceof Tree) {
				Tree t = (Tree) node;
				return operator == t.operator && Util.equals(left, t.left) && Util.equals(right, t.right);
			} else
				return false;
		} else
			return false;
	}

	// This method violates the immutable property of the tree.
	// Only used by cloner for performance purpose.
	public static void forceSetRight(Tree tree, Node right) {
		tree.right = right;
	}

	public Operator getOperator() {
		return operator;
	}

	public Node getLeft() {
		return left;
	}

	public Node getRight() {
		return right;
	}

}
