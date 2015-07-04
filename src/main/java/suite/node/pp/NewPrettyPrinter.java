package suite.node.pp;

import java.util.HashSet;
import java.util.Set;

import suite.Suite;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.Operator;
import suite.node.io.TermOp;

/**
 * Pretty printer with language context.
 * 
 * @author ywsing
 */
public class NewPrettyPrinter {

	private int lineLength = 80;
	private String ind = "  ";

	private LengthEstimator lengthEstimator = new LengthEstimator(lineLength);
	private Set<Integer> set = new HashSet<>();
	private StringBuilder sb = new StringBuilder();

	public String prettyPrint(Node node) {
		lengthEstimator.estimateLengths(node);
		format(node, 0, "");
		return sb.toString();
	}

	private void format(Node node, int parentPrec, String indent) {
		Integer objectId = System.identityHashCode(node);

		// Avoids infinite recursion if object is recursive
		if (set.add(objectId)) {
			format0(node, parentPrec, indent);
			set.remove(objectId);
		} else
			sb.append("<<recurse>>");
	}

	private void format0(Node node, int parentPrec, String indent) {
		format0(node, parentPrec, indent, "");
	}

	private void format0(Node node, int parentPrec, String indent, String prefix) {
		Tree tree;

		if ((tree = Tree.decompose(node)) != null) {
			Operator operator = tree.getOperator();
			int prec = operator.getPrecedence();
			boolean isParenthesesRequired = operator != null ? prec <= parentPrec : false;
			String indent1 = indent + ind;
			Node m[];

			if (isParenthesesRequired) {
				format0(node, 0, indent, concatWithSpace(prefix, "("));
				sb.append(indent + ")");
			} else if (operator == TermOp.NEXT__) {
				format0(tree.getLeft(), TermOp.getLeftPrec(operator), indent, prefix);
				sb.append(indent + "#\n");
				format0(tree.getRight(), TermOp.getRightPrec(operator), indent, "");
			} else if (operator == TermOp.IS____) {
				format0(tree.getLeft(), TermOp.getLeftPrec(operator), indent, prefix);
				format0(tree.getRight(), TermOp.getRightPrec(operator), indent1, operator.getName());
			} else if (operator == TermOp.BIGAND || operator == TermOp.BIGOR_) {
				format0(tree.getLeft(), TermOp.getLeftPrec(operator), indent, prefix);
				format0(tree.getRight(), TermOp.getRightPrec(operator), indent, operator.getName());
			} else if (operator == TermOp.AND___ || operator == TermOp.OR____) {
				format0(tree.getLeft(), prec, indent, prefix);
				node = tree.getRight();
				while ((tree = Tree.decompose(node)) != null && tree.getOperator() == operator) {
					format0(tree.getLeft(), prec, indent1, operator.getName());
					node = tree.getRight();
				}
				format0(node, prec, indent1, operator.getName());
			} else if ((m = Suite.matcher("if .0 then .1 else .2").apply(node)) != null //
					&& lengthEstimator.getEstimatedLength(node) > lineLength) {
				format0(m[0], prec, indent, concatWithSpace(prefix, "if"));
				format0(m[1], prec, indent, "then");
				format0(m[2], prec, indent, "else");
			} else if ((m = Suite.matcher("not .0").apply(node)) != null //
					&& lengthEstimator.getEstimatedLength(node) > lineLength)
				format0(m[0], prec, indent, concatWithSpace(prefix, "not"));
			else if ((m = Suite.matcher("once .0").apply(node)) != null //
					&& lengthEstimator.getEstimatedLength(node) > lineLength)
				format0(m[0], prec, indent, concatWithSpace(prefix, "once"));
			else
				format0(node, indent, prefix);
		} else
			format0(node, indent, prefix);
	}

	private void format0(Node node, String indent, String prefix) {
		sb.append(indent + prefix.trim());
		if (!prefix.isEmpty())
			sb.append(" ");
		sb.append(Formatter.dump(node));
		sb.append("\n");
	}

	private String concatWithSpace(String s0, String s1) {
		if (!s0.isEmpty() && !s1.isEmpty())
			return s0 + " " + s1;
		else
			return s0 + s1;
	}

}
