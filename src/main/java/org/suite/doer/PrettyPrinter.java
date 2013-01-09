package org.suite.doer;

import java.util.HashMap;
import java.util.Map;

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

	private static final int LINELENGTH = 80; // Estimated
	private static final String INDENTSPACES = "    ";

	public String prettyPrint(Node node) {
		estimateStringLengths(node);
		prettyPrint0(node);
		return sb.toString();
	}

	private void prettyPrint0(Node node) {
		int x = getX();
		int length = getEstimatedStringLength(node);

		// Line too long?
		if (node instanceof Tree && x + length > LINELENGTH) {
			Tree tree = (Tree) node;
			Operator op = tree.getOperator();

			if (isLookingLikeList(op, node))
				prettyPrintList(op, node);
			else {
				Node left = tree.getLeft();
				Node right = tree.getRight();

				Tree tree1 = Tree.decompose(right, op);
				Node r0 = tree1 != null ? tree1.getLeft() : null;
				Integer es0 = getEstimatedStringLength(left);
				Integer es1 = r0 != null ? getEstimatedStringLength(r0) : null;
				int opLength = op.getName().length();

				// Breaks "a + b + xxx" in the second operator
				if (op.getAssoc() == Assoc.RIGHT //
						&& es1 != null //
						&& x + es0 + es1 + opLength < LINELENGTH) {
					append(Formatter.dump(left) + op.getName());
					prettyPrint0(right);
				} else { // Breaks after the operator
					prettyPrint0(left);
					incrementIndent();
					appendOperator(tree.getOperator());
					prettyPrint0(right);
					decrementIndent();
				}
			}
		} else
			append(Formatter.dump(node)); // Space sufficient
	}

	private void prettyPrintList(Operator op, Node node) {
		if (node instanceof Tree) {
			Tree tree = (Tree) node;

			if (tree.getOperator() == op) {
				boolean isLeftAssoc = op.getAssoc() == Assoc.LEFT;

				if (isLeftAssoc)
					prettyPrintList(op, tree.getLeft());
				else
					prettyPrint0(tree.getLeft());

				appendOperator(op);

				if (!isLeftAssoc)
					prettyPrintList(op, tree.getRight());
				else
					prettyPrint0(tree.getRight());

				return;
			}
		}

		prettyPrint0(node);
	}

	private int estimateStringLengths(Node node) {
		int key = getKey(node);
		Integer length = lengthByIds.get(key);

		if (length == null) {
			int len;

			if (node instanceof Tree) {
				Tree tree = (Tree) node;
				int len0 = estimateStringLengths(tree.getLeft());
				int len1 = estimateStringLengths(tree.getRight());
				int opLength = tree.getOperator().getName().length();
				len = len0 + len1 + opLength; // Rough estimation
			} else
				len = Formatter.dump(node).length();

			length = len;
			lengthByIds.put(key, length);
		}

		return length;
	}

	private Integer getEstimatedStringLength(Node node) {
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

	private void appendOperator(Operator op) {
		String opName = op.getName();
		if (op == TermOp.AND___ || op == TermOp.OR____)
			opName = opName + " ";

		nl();
		append(FormatUtil.leftTrim(opName));
		if (op == TermOp.NEXT__)
			nl();
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
		int pos = sb.length();

		while (--pos > 0) {
			char c = sb.charAt(pos);
			if (!Character.isWhitespace(c))
				return false;
			if (c == '\n')
				break;
		}

		return true;
	}

	private int getX() {
		int length = sb.length(), pos = length;
		while (--pos > 0 && sb.charAt(pos) != '\n')
			;
		return length - pos;
	}

	private void nl() {
		append("\n");
		pre(indent);
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
