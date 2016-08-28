package suite.node;

import suite.node.io.Operator;
import suite.node.io.TermOp;

public class TreeAnd extends Tree {

	private static Operator operator = TermOp.AND___;

	public TreeAnd(Node left, Node right) {
		super(left, right);
	}

	@Override
	public Operator getOperator() {
		return operator;
	}

}
