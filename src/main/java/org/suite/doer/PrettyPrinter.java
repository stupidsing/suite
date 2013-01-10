package org.suite.doer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.parser.Operator;
import org.parser.Operator.Assoc;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Tree;
import org.util.FormatUtil;

public class PrettyPrinter {

	private int indent;
	private Map<Integer, Integer> lengthByIds = new HashMap<Integer, Integer>();
	private StringBuilder sb = new StringBuilder();
	private int nLines = 0;

	private static final int LINELENGTH = 80; // Estimated
	private static final String INDENTSPACES = "    ";
	private static final Set<Node> LINEBREAKERKEYWORDS = new HashSet<Node>(
			Arrays.asList(Atom.create("else-if")));

	private static final Set<Operator> LINEBREAKAFTEROPERATORS = new HashSet<Operator>(
			Arrays.asList(TermOp.CONTD_, TermOp.FUN___));

	public String prettyPrint(Node node) {
		estimateLengths(node);
		prettyPrint0(node, 0);
		return sb.toString();
	}

	private void prettyPrint0(Node node, int prec0) {
		node = node.finalNode();
		int x = getX(), y = getY();
		int length = getEstimatedLength(node);

		// Line too long?
		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Operator op = tree.getOperator();
			int prec = op.getPrecedence();
			boolean needPars = prec < prec0;

			if (needPars)
				append("(");

			if (x + length > LINELENGTH)
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
					Integer es0 = getEstimatedLength(left);
					Integer es1 = r0 != null ? getEstimatedLength(r0) : null;
					int opLength = op.getName().length();

					// Breaks "a + b + xxx" in the second operator
					if (assoc == Assoc.RIGHT //
							&& es1 != null //
							&& x + es0 + es1 + opLength < LINELENGTH) {
						prettyPrint0(left, leftPrec);
						int yOp = appendOperator(op);
						prettyPrint0(right, rightPrec);
						closeBraces(op, yOp);
					} else { // Breaks after the operator
						boolean incRightIndent = Tree.decompose(right, op) == null;

						prettyPrint0(left, leftPrec);

						if (incRightIndent)
							incrementIndent();

						int yOp = appendOperatorLineFeed(op);
						prettyPrint0(right, rightPrec);
						closeBraces(op, yOp);

						if (incRightIndent)
							decrementIndent();
					}
				}
			else
				append(Formatter.dump(node));

			if (needPars) {
				if (y != getY())
					nl();
				append(")");
			}
		} else {
			if (LINEBREAKERKEYWORDS.contains(node) && !isLineBegin())
				nl();

			append(Formatter.dump(node)); // Space sufficient
		}
	}

	private void prettyPrintList(Operator op, Node node) {
		int prec = op.getPrecedence(), prec1 = prec - 1;

		if (node instanceof Tree) {
			Tree tree = (Tree) node;

			if (tree.getOperator() == op) {
				boolean isLeftAssoc = op.getAssoc() == Assoc.LEFT;

				if (isLeftAssoc)
					prettyPrintList(op, tree.getLeft());
				else
					prettyPrint0(tree.getLeft(), prec1);

				int yOp = appendOperatorLineFeed(op);

				if (!isLeftAssoc)
					prettyPrintList(op, tree.getRight());
				else
					prettyPrint0(tree.getRight(), prec1);

				closeBraces(op, yOp);
				return;
			}
		}

		prettyPrint0(node, prec);
	}

	private void closeBraces(Operator op, int y) {
		if (op == TermOp.BRACES) {
			if (y != getY())
				nl();
			append("}");
		}
	}

	private int estimateLengths(Node node) {
		return estimateLengths(node, 0);
	}

	private int estimateLengths(Node node, int prec0) {
		node = node.finalNode();
		int key = getKey(node);
		Integer length = lengthByIds.get(key);

		if (length == null) {
			int len;

			if (node instanceof Tree) {
				Tree tree = (Tree) node;

				Operator op = tree.getOperator();
				int prec = op.getPrecedence();
				Assoc assoc = op.getAssoc();
				int leftPrec = prec - (assoc == Assoc.LEFT ? 1 : 0);
				int rightPrec = prec - (assoc == Assoc.RIGHT ? 1 : 0);
				if (op == TermOp.BRACES)
					leftPrec = rightPrec = 0;

				int len0 = estimateLengths(tree.getLeft(), leftPrec);
				int len1 = estimateLengths(tree.getRight(), rightPrec);
				int opLength = op.getName().length();

				// Rough estimation
				len = len0 + len1 + opLength + (prec < prec0 ? 2 : 0);
			} else
				len = Formatter.dump(node).length();

			length = len;
			lengthByIds.put(key, length);
		}

		return length;
	}

	private Integer getEstimatedLength(Node node) {
		return lengthByIds.get(getKey(node));
	}

	private boolean isLookingLikeList(Operator op, Node node) {
		if (node instanceof Tree) {
			Tree tree = (Tree) node;

			if (tree.getOperator() == op) {
				boolean isLeftAssoc = op.getAssoc() == Assoc.LEFT;
				Node child = isLeftAssoc ? tree.getLeft() : tree.getRight();
				return isLookingLikeList(op, child);
			}
		}

		return op != TermOp.TUPLE_
				&& (op == TermOp.AND___ || op == TermOp.OR____ || node == Atom.nil);
	}

	private int appendOperatorLineFeed(Operator op) {
		boolean lineBreakAfterOp = LINEBREAKAFTEROPERATORS.contains(op);
		if (!lineBreakAfterOp)
			nl();
		int y = appendOperator(op);
		if (lineBreakAfterOp || op == TermOp.NEXT__)
			nl();
		return y;
	}

	private int appendOperator(Operator op) {
		String name = op.getName();
		name += op == TermOp.AND___ || op == TermOp.OR____ ? " " : "";
		if (isLineBegin())
			name = FormatUtil.leftTrim(name);
		append(name);
		return getY();
	}

	private void incrementIndent() {
		indent++;
		if (isLineBegin())
			append(INDENTSPACES);
	}

	private void decrementIndent() {
		indent--;
	}

	private boolean isLineBegin() {
		boolean result = true;
		String l = sb.substring(getLineBeginPosition(), sb.length());
		for (char c : l.toCharArray())
			result &= Character.isWhitespace(c);
		return result;
	}

	private int getX() {
		return sb.length() - getLineBeginPosition();
	}

	private int getY() {
		return nLines;
	}

	private void nl() {
		append("\n");
		pre(indent);
		nLines++;
	}

	private int getLineBeginPosition() {
		int pos = sb.length();
		while (--pos > 0 && sb.charAt(pos) != '\n')
			;
		return pos + 1;
	}

	private void pre(int indent) {
		for (int i = 0; i < indent; i++)
			append(INDENTSPACES);
	}

	private void append(String s) {
		sb.append(s);
	}

	private int getKey(Node node) {
		return System.identityHashCode(node);
	}

}
