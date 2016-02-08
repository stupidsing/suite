package suite.node;

import java.util.List;
import java.util.Objects;

import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.streamlet.Outlet;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class Tree extends Node {

	private Operator operator;
	private Node left, right;

	public static Tree decompose(Node node) {
		return node instanceof Tree ? (Tree) node : null;
	}

	public static Tree decompose(Node node, Operator operator) {
		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			if (tree.operator == operator)
				return tree;
		}
		return null;
	}

	public static Streamlet<Node> iter(Node node) {
		return iter(node, TermOp.AND___);
	}

	public static Streamlet<Node> iter(Node node0, Operator operator) {
		return new Streamlet<>(() -> Outlet.from(new Source<Node>() {
			private Node node = node0;

			public Node source() {
				Tree tree = Tree.decompose(node, operator);
				if (tree != null) {
					node = tree.getRight();
					return tree.getLeft();
				} else
					return null;
			}
		}));
	}

	public static Node of(Operator operator, List<Node> nodes) {
		Node result = Atom.NIL;
		int i = nodes.size();
		while (0 <= --i)
			result = of(operator, nodes.get(i), result);
		return result;
	}

	public static Tree of(Operator operator, Node left, Node right) {
		return new Tree(operator, left, right);
	}

	private Tree(Operator operator, Node left, Node right) {
		this.operator = operator;
		this.left = left;
		this.right = right;
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
		if (Util.clazz(object) == Tree.class) {
			Tree t = (Tree) object;
			return operator == t.operator && Objects.equals(left, t.left) && Objects.equals(right, t.right);
		} else
			return false;
	}

	// These methods violate the immutable property of the tree. Should only
	// used by cloner for performance purpose.
	public static void forceSetLeft(Tree tree, Node left) {
		tree.left = left;
	}

	public static void forceSetRight(Tree tree, Node right) {
		tree.right = right;
	}

	public Operator getOperator() {
		return operator;
	}

	public Node getLeft() {
		return left.finalNode();
	}

	public Node getRight() {
		return right.finalNode();
	}

}
