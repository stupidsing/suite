package suite.node;

import java.util.Objects;

import primal.Ob;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.tree.TreeAnd;
import suite.node.tree.TreeOp;
import suite.node.tree.TreeOr;
import suite.node.tree.TreeTuple;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Puller;
import suite.streamlet.Streamlet;

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

	// these methods violate the immutable property of the tree. Used by cloner
	// for
	// performance purpose.
	public static void forceSetLeft(Tree tree, Node left) {
		tree.left = left;
	}

	public static void forceSetRight(Tree tree, Node right) {
		tree.right = right;
	}

	public static Streamlet<Node> read(Node node) {
		return read(node, TermOp.AND___);
	}

	public static Streamlet<Node> read(Node node0, Operator operator) {
		return read(node0, n -> Tree.decompose(n, operator));
	}

	public static Streamlet<Node> read(Node node0, Fun<Node, Tree> fun) {
		return new Streamlet<>(() -> Puller.of(new Source<Node>() {
			private Node node = node0;

			public Node g() {
				var tree = fun.apply(node);
				if (tree != null) {
					node = tree.getRight();
					return tree.getLeft();
				} else
					return null;
			}
		}));
	}

	public static Tree of(Operator operator, Node left, Node right) {
		if (operator == TermOp.AND___)
			return ofAnd(left, right);
		else if (operator == TermOp.OR____)
			return ofOr(left, right);
		else if (operator == TermOp.TUPLE_)
			return TreeTuple.of(left, right);
		else
			return new TreeOp(operator, left, right);
	}

	public static Tree ofAnd(Node left, Node right) {
		return new TreeAnd(left, right);
	}

	public static Tree ofOr(Node left, Node right) {
		return new TreeOr(left, right);
	}

	protected Tree(Node left, Node right) {
		this.left = left;
		this.right = right;
	}

	public boolean childrenEquals(Tree t) {
		return Ob.equals(getLeft(), t.getLeft()) && Ob.equals(getRight(), t.getRight());
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
