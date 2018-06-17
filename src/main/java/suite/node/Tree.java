package suite.node;

import java.util.List;
import java.util.Objects;

import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.tree.TreeAnd;
import suite.node.tree.TreeOp;
import suite.node.tree.TreeOr;
import suite.node.tree.TreeTuple;
import suite.streamlet.Outlet;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;

public abstract class Tree extends Node {

	private Node left, right;

	public static Tree decompose(Node node) {
		return node instanceof Tree ? (Tree) node : null;
	}

	public static Tree decompose(Node node, Operator operator) {
		if (node instanceof Tree) {
			var tree = (Tree) node;
			return tree.getOperator() == operator ? tree : null;
		} else
			return null;
	}

	// these methods violate the immutable property of the tree. Used by cloner for
	// performance purpose.
	public static void forceSetLeft(Tree tree, Node left) {
		tree.left = left;
	}

	public static void forceSetRight(Tree tree, Node right) {
		tree.right = right;
	}

	public static Streamlet<Node> iter(Node node) {
		return iter(node, TermOp.AND___);
	}

	public static Streamlet<Node> iter(Node node0, Operator operator) {
		return new Streamlet<>(() -> Outlet.of(new Source<Node>() {
			private Node node = node0;

			public Node source() {
				var tree = Tree.decompose(node, operator);
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
		var i = nodes.size();
		while (0 <= --i)
			result = of(operator, nodes.get(i), result);
		return result;
	}

	public static Tree of(Operator operator, Node left, Node right) {
		if (operator == TermOp.AND___)
			return TreeAnd.of(left, right);
		else if (operator == TermOp.OR____)
			return TreeOr.of(left, right);
		else if (operator == TermOp.TUPLE_)
			return TreeTuple.of(left, right);
		else
			return new TreeOp(operator, left, right);
	}

	protected Tree(Node left, Node right) {
		this.left = left;
		this.right = right;
	}

	public boolean childrenEquals(Tree t) {
		return Objects.equals(getLeft(), t.getLeft()) && Objects.equals(getRight(), t.getRight());
	}

	@Override
	public int hashCode() {
		var h = 7;
		h = h * 31 + Objects.hashCode(getLeft());
		h = h * 31 + Objects.hashCode(getOperator());
		h = h * 31 + Objects.hashCode(getRight());
		return h;
	}

	public Node getLeft() {
		return left.finalNode();
	}

	public Node getRight() {
		return right.finalNode();
	}

	public abstract Operator getOperator();

}
