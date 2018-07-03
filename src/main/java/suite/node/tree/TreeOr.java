package suite.node.tree;

import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.object.Object_;

public class TreeOr extends Tree {

	private static Operator operator = TermOp.OR____;

	public static TreeOr of(Node left, Node right) {
		return new TreeOr(left, right);
	}

	private TreeOr(Node left, Node right) {
		super(left, right);
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == getClass() ? childrenEquals((Tree) object) : false;
	}

	@Override
	public Operator getOperator() {
		return operator;
	}

}
