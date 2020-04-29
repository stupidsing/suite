package suite.node.pp;

import java.util.HashSet;
import java.util.Set;

import suite.Suite;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.TermOp;

/**
 * Pretty printer with language context.
 * 
 * @author ywsing
 */
public class NewPrettyPrinter {

	private int lineLength = 80;
	private String ind = "  ";

	private EstimateLength estimateLength;
	private Set<Integer> set = new HashSet<>();
	private StringBuilder sb = new StringBuilder();

	public String prettyPrint(Node node) {
		estimateLength = new EstimateLength(lineLength, node);
		format(node, 0, "");
		return sb.toString();
	}

	private void format(Node node, int parentPrec, String indent) {
		var objectId = System.identityHashCode(node);

		// avoids infinite recursion if object is recursive
		if (set.add(objectId)) {
			format_(node, parentPrec, indent);
			set.remove(objectId);
		} else
			sb.append("<<recurse>>");
	}

	private void format_(Node node, int parentPrec, String indent) {
		format_(node, parentPrec, indent, "");
	}

	private void format_(Node node, int parentPrec, String indent, String prefix) {
		Tree tree;

		if ((tree = Tree.decompose(node)) != null) {
			var operator = tree.getOperator();
			var prec = operator.precedence();
			var isParenthesesRequired = operator != null ? prec <= parentPrec : false;
			var indent1 = indent + ind;
			Node[] m;

			if (isParenthesesRequired) {
				format_(node, 0, indent, concatWithSpace(prefix, "("));
				sb.append(indent + ")");
			} else if (operator == TermOp.NEXT__) {
				format_(tree.getLeft(), TermOp.getLeftPrec(operator), indent, prefix);
				sb.append(indent + "#\n");
				format_(tree.getRight(), TermOp.getRightPrec(operator), indent, "");
			} else if (operator == TermOp.IS____) {
				format_(tree.getLeft(), TermOp.getLeftPrec(operator), indent, prefix);
				format_(tree.getRight(), TermOp.getRightPrec(operator), indent1, operator.name_());
			} else if (operator == TermOp.BIGAND || operator == TermOp.BIGOR_) {
				format_(tree.getLeft(), TermOp.getLeftPrec(operator), indent, prefix);
				format_(tree.getRight(), TermOp.getRightPrec(operator), indent, operator.name_());
			} else if (operator == TermOp.AND___ || operator == TermOp.OR____) {
				format_(tree.getLeft(), prec, indent, prefix);
				node = tree.getRight();
				while ((tree = Tree.decompose(node)) != null && tree.getOperator() == operator) {
					format_(tree.getLeft(), prec, indent1, operator.name_());
					node = tree.getRight();
				}
				format_(node, prec, indent1, operator.name_());
			} else if ((m = Suite.pattern("if .0 then .1 else .2").match(node)) != null
					&& lineLength < estimateLength.getEstimatedLength(node)) {
				format_(m[0], prec, indent, concatWithSpace(prefix, "if"));
				format_(m[1], prec, indent, "then");
				format_(m[2], prec, indent, "else");
			} else if ((m = Suite.pattern("not .0").match(node)) != null
					&& lineLength < estimateLength.getEstimatedLength(node))
				format_(m[0], prec, indent, concatWithSpace(prefix, "not"));
			else if ((m = Suite.pattern("once .0").match(node)) != null
					&& lineLength < estimateLength.getEstimatedLength(node))
				format_(m[0], prec, indent, concatWithSpace(prefix, "once"));
			else
				format_(node, indent, prefix);
		} else
			format_(node, indent, prefix);
	}

	private void format_(Node node, String indent, String prefix) {
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
