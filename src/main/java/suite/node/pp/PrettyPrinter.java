package suite.node.pp;

import java.util.Set;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.util.FormatUtil;
import suite.util.String_;
import suite.util.To;

public class PrettyPrinter {

	private int indent;
	private StringBuilder sb = new StringBuilder();
	private int nLines = 0;
	private int currentLineIndent = 0;

	private static int lineLength = 96; // estimated
	private static int squeezeLineLength = 8;
	private static String indentSpaces = "    ";

	private static Node lineBreakBeforeKeyword = Atom.of("else-if");
	private static Node preferLineBreakBeforeKeyword = Atom.of("else");
	private static Set<Operator> lineBreakAfterOperators = To.set(TermOp.BRACES, TermOp.CONTD_, TermOp.FUN___);

	private LengthEstimator lengthEstimator = new LengthEstimator(lineLength);

	private static class OperatorPosition {
		private int indent;
		private int y;

		public OperatorPosition(int indent, int y) {
			this.indent = indent;
			this.y = y;
		}
	}

	public String prettyPrint(Node node) {
		lengthEstimator.estimateLengths(node);
		prettyPrint_(node, null, 0);
		return sb.toString();
	}

	// op0 for avoiding unnecessary indenting; prec0 for parenthesizing
	private void prettyPrint_(Node node, Operator op0, int prec0) {
		int x = getX(), y = getY();
		int length = lengthEstimator.getEstimatedLength(node);

		// line too long?
		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Operator op = tree.getOperator();
			int prec = op.getPrecedence();
			boolean isNeedPars = prec <= prec0;
			int parsIndent = 0, parsIndent0 = 0;

			if (isNeedPars) {
				parsIndent = currentLineIndent;
				parsIndent0 = incrementIndent();
				append("(");
			}

			if (lineLength < x + length)
				if (isLookingLikeList(op, node))
					prettyPrintList(op, node);
				else {
					Node left = tree.getLeft();
					Node right = tree.getRight();
					Assoc assoc = op.getAssoc();

					int leftPrec = prec - (assoc == Assoc.LEFT ? 1 : 0);
					int rightPrec = prec - (assoc == Assoc.RIGHT ? 1 : 0);
					if (op == TermOp.BRACES)
						leftPrec = rightPrec = 0;

					Tree tree1 = Tree.decompose(right, op);
					Node r0 = tree1 != null ? tree1.getLeft() : null;
					int es0 = lengthEstimator.getEstimatedLength(left);
					int es1 = r0 != null ? lengthEstimator.getEstimatedLength(r0) : lineLength;
					int opLength = op.getName().length();

					// breaks "a + b + xxx" in the second operator
					if (assoc == Assoc.RIGHT //
							&& x + es0 + es1 + opLength < lineLength //
							&& r0 != preferLineBreakBeforeKeyword) {
						prettyPrint_(left, op, leftPrec);
						OperatorPosition opPos = appendOperator(op);
						prettyPrint_(right, op, rightPrec);
						closeBraces(op, opPos);
					} else { // breaks after the operator
						boolean isIncRightIndent = op != op0;
						int indent0 = 0;

						prettyPrint_(left, op, leftPrec);

						if (isIncRightIndent)
							indent0 = incrementIndent();

						OperatorPosition opPos;
						if (getLineSize() + lengthEstimator.getEstimatedLength(right) < squeezeLineLength)
							opPos = appendOperator(op);
						else
							opPos = appendOperatorLineFeed(op);

						prettyPrint_(right, op, rightPrec);
						closeBraces(op, opPos);

						if (isIncRightIndent)
							revertIndent(indent0);
					}
				}
			else
				append(Formatter.dump(node));

			if (isNeedPars) {
				if (y != getY())
					nl(parsIndent);
				append(")");
				revertIndent(parsIndent0);
			}
		} else {
			if (node == lineBreakBeforeKeyword && !isLineBegin())
				nl();

			append(Formatter.dump(node)); // space sufficient
		}
	}

	private void prettyPrintList(Operator op, Node node) {
		int prec = op.getPrecedence(), prec1 = prec - 1;

		if (node instanceof Tree) {
			Tree tree = (Tree) node;

			if (tree.getOperator() == op) {
				boolean isLeftAssoc = op.getAssoc() == Assoc.LEFT;
				OperatorPosition opPos;

				if (isLeftAssoc) {
					prettyPrintList(op, tree.getLeft());
					opPos = appendOperatorLineFeed(op);
					prettyPrintIndented(tree.getRight(), prec1);
				} else {
					prettyPrintIndented(tree.getLeft(), prec1);
					opPos = appendOperatorLineFeed(op);
					prettyPrintList(op, tree.getRight());
				}

				closeBraces(op, opPos);
				return;
			}
		}

		// if (node != Atom.nil) // suppress list termination
		prettyPrint_(node, op, prec);
	}

	private void prettyPrintIndented(Node left, int prec) {
		int indent0 = incrementIndent();
		prettyPrint_(left, null, prec);
		revertIndent(indent0);
	}

	private void closeBraces(Operator op, OperatorPosition opPos) {
		if (op == TermOp.BRACES) {
			if (opPos.y != getY())
				nl(opPos.indent);
			append("}");
		}
	}

	private boolean isLookingLikeList(Operator op, Node node) {
		Tree tree = Tree.decompose(node);

		if (tree != null && tree.getOperator() == op) {
			boolean isLeftAssoc = op.getAssoc() == Assoc.LEFT;
			Node child = isLeftAssoc ? tree.getLeft() : tree.getRight();
			return isLookingLikeList(op, child);
		}

		return op != TermOp.TUPLE_ && (op == TermOp.AND___ || op == TermOp.OR____ || node == Atom.NIL);
	}

	private OperatorPosition appendOperatorLineFeed(Operator op) {
		boolean isLineFeedAfterOp = lineBreakAfterOperators.contains(op);
		if (!isLineFeedAfterOp)
			nl();
		OperatorPosition result = appendOperator(op);
		if (isLineFeedAfterOp || op == TermOp.NEXT__)
			nl();
		return result;
	}

	private OperatorPosition appendOperator(Operator op) {
		String name = op.getName();
		name = (op == TermOp.BRACES ? " " : "") + name;
		name += op == TermOp.AND___ || op == TermOp.OR____ ? " " : "";
		if (isLineBegin())
			name = FormatUtil.trimLeft(name);
		append(name);
		return new OperatorPosition(currentLineIndent, getY());
	}

	private int incrementIndent() { // would not jump by two
		int indent0 = indent;
		indent = Math.min(indent, currentLineIndent) + 1;
		return indent0;
	}

	private void revertIndent(int indent0) {
		indent = indent0;
	}

	private boolean isLineBegin() {
		boolean result = true;
		String l = sb.substring(getLineBeginPosition(), getCurrentPosition());
		for (char c : String_.chars(l))
			result &= Character.isWhitespace(c);
		return result;
	}

	private int getLineSize() {
		return getCurrentPosition() - getLineContentBeginPosition();
	}

	private int getX() {
		return getCurrentPosition() - getLineBeginPosition();
	}

	private int getY() {
		return nLines;
	}

	private int getLineContentBeginPosition() {
		int pos = getLineBeginPosition();
		while (pos < getCurrentPosition() && Character.isWhitespace(sb.charAt(pos)))
			pos++;
		return pos;
	}

	private int getLineBeginPosition() {
		int pos = getCurrentPosition();
		while (0 < --pos && sb.charAt(pos) != '\n')
			;
		return pos + 1;
	}

	private int getCurrentPosition() {
		return sb.length();
	}

	private void nl() {
		nl(indent);
	}

	private void nl(int indent) {
		append("\n");
		nLines++;
		pre(currentLineIndent = indent);
	}

	private void pre(int indent) {
		for (int i = 0; i < indent; i++)
			append(indentSpaces);
	}

	private void append(String s) {
		sb.append(s);
	}

}
