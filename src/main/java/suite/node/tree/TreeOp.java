package suite.node.tree;

import primal.Verbs.Get;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;

public class TreeOp extends Tree {

	private Operator operator;

	public TreeOp(Operator operator, Node left, Node right) {
		super(left, right);
		this.operator = operator;
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == TreeOp.class) {
			var t = (TreeOp) object;
			return operator == t.operator && childrenEquals(t);
		} else
			return false;
	}

	@Override
	public Operator getOperator() {
		return operator;
	}

}
