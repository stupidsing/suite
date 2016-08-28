package suite.node;

import suite.node.io.Operator;
import suite.node.io.TermOp;

public class TreeOr extends Tree {

	private static Operator operator = TermOp.OR____;

	public TreeOr(Node left, Node right) {
		super(left, right);
	}

	@Override
	public Operator getOperator() {
		return operator;
	}

}
