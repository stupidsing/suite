package suite.node.tree;

import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.util.Object_;

public class TreeOp extends Tree {

	private Operator operator;

	public TreeOp(Operator operator, Node left, Node right) {
		super(left, right);
		this.operator = operator;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == TreeOp.class) {
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
