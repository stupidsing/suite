package suite.node;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.Util;

public class Tree extends Node {

	private Operator operator;
	private Node left, right;

	private Tree(Operator operator, Node left, Node right) {
		this.operator = operator;
		this.left = left;
		this.right = right;
	}

	public static Node[] getParameters(Node node, int n) {
		Node params[] = new Node[n];
		Tree tree;

		for (int i = 0; i < n - 1; i++)
			if ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
				params[i] = tree.getLeft();
				node = tree.getRight();
			} else
				throw new RuntimeException("Not enough parameters");

		params[n - 1] = node;
		return params;
	}

	public static Node list(Operator operator, List<Node> nodes) {
		Node result = Atom.NIL;
		int i = nodes.size();
		while (--i >= 0)
			result = of(operator, nodes.get(i), result);
		return result;
	}

	public static Iterable<Node> iter(Node node) {
		return iter(node, TermOp.AND___);
	}

	public static Iterable<Node> iter(Node node0, Operator operator) {
		return Util.iter(new Iterator<Node>() {
			private Tree tree = decompose(node0, operator);

			public boolean hasNext() {
				return tree != null;
			}

			public Node next() {
				Node next = tree.getLeft();
				tree = Tree.decompose(tree.getRight(), operator);
				return next;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		});
	}

	public static Tree of(Operator operator, Node left, Node right) {
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
		result = 31 * result + Objects.hashCode(left);
		result = 31 * result + Objects.hashCode(operator);
		result = 31 * result + Objects.hashCode(right);
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Node) {
			Node node = ((Node) object).finalNode();
			if (Util.clazz(node) == Tree.class) {
				Tree t = (Tree) node;
				return operator == t.operator && Objects.equals(left, t.left) && Objects.equals(right, t.right);
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
