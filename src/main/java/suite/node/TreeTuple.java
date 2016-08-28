package suite.node;

import suite.node.io.Operator;
import suite.node.io.TermOp;

public class TreeTuple extends Tree {

	private static Operator operator = TermOp.TUPLE_;

	public TreeTuple(Node left, Node right) {
		super(left, right);
	}

	@Override
	public Operator getOperator() {
		return operator;
	}

}
