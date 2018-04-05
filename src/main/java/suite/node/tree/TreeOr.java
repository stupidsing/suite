package suite.node.tree;

import java.util.Objects;

import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.Object_;

public class TreeOr extends Tree {

	private static Operator operator = TermOp.OR____;

	public TreeOr(Node left, Node right) {
		super(left, right);
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == TreeOr.class) {
			var t = (TreeOr) object;
			return Objects.equals(getLeft(), t.getLeft()) && Objects.equals(getRight(), t.getRight());
		} else
			return false;
	}

	@Override
	public int hashCode() {
		var h = 7;
		h = h * 31 + Objects.hashCode(getLeft());
		h = h * 31 + Objects.hashCode(operator);
		h = h * 31 + Objects.hashCode(getRight());
		return h;
	}

	@Override
	public Operator getOperator() {
		return operator;
	}

}
