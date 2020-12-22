package suite.node.tree;

import primal.Verbs.Get;
import primal.parser.Operator;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.BaseOp;

public class TreeOr extends Tree {

	private static Operator operator = BaseOp.OR____;

	public TreeOr(Node left, Node right) {
		super(left, right);
	}

	@Override
	public boolean equals(Object object) {
		return Get.clazz(object) == getClass() ? childrenEquals((Tree) object) : false;
	}

	@Override
	public Operator getOperator() {
		return operator;
	}

}
