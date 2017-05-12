package suite.node.tree;

import java.util.Objects;

import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.Object_;

public class TreeTuple extends Tree {

	private static Operator operator = TermOp.TUPLE_;

	public TreeTuple(Node left, Node right) {
		super(left, right);
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == TreeTuple.class) {
			TreeTuple t = (TreeTuple) object;
			return Objects.equals(getLeft(), t.getLeft()) && Objects.equals(getRight(), t.getRight());
		} else
			return false;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + Objects.hashCode(getLeft());
		result = 31 * result + Objects.hashCode(operator);
		result = 31 * result + Objects.hashCode(getRight());
		return result;
	}

	@Override
	public Operator getOperator() {
		return operator;
	}

}
