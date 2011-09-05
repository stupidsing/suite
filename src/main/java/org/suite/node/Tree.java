package org.suite.node;

import org.parser.Operator;
import org.util.Util;

public class Tree extends Node {

	private Operator operator;
	private Node left, right;

	public Tree(Operator operator) {
		this(operator, Atom.nil, Atom.nil);
	}

	public Tree(Operator operator, Node node) {
		this(operator, node, Atom.nil);
	}

	public Tree(Operator operator, Node left, Node right) {
		this.operator = operator;
		this.left = left;
		this.right = right;
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
		if (object instanceof Tree) {
			Tree t = (Tree) object;
			return operator == t.operator //
					&& Util.equals(left, t.left) && Util.equals(right, t.right);
		} else
			return false;
	}

	public static Tree decompose(Node node) {
		node = node.finalNode();
		return (node instanceof Tree) ? (Tree) node : null;
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
