package suite.node;

import suite.node.io.Operator;

public class TreeOp extends Tree {

	private Operator operator;

	public TreeOp(Operator operator, Node left, Node right) {
		super(left, right);
		this.operator = operator;
	}

	public Operator getOperator() {
		return operator;
	}

}
