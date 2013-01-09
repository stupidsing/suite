package org.suite.doer;

import java.util.HashMap;
import java.util.Map;

import org.parser.Operator;
import org.parser.Operator.Assoc;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Tree;

public class PrettyPrinter {

	private int indent;
	private Map<Integer, Integer> lengthByIds = new HashMap<Integer, Integer>();
	private StringBuilder sb = new StringBuilder();

	private static final int LINELENGTH = 128; // Estimated
	private static final String INDENTSPACES = "    ";

	public String prettyPrint(Node node) {
		estimateLeafStringLengths(node);
		prettyPrint0(node);
		return sb.toString();
	}

	private void prettyPrint0(Node node) {
		int length = lengthByIds.get(getKey(node));

		if (node instanceof Tree && length > LINELENGTH - getX()) {
			Tree tree = (Tree) node;
			Operator op = tree.getOperator();

			if (isLookingLikeList(op, node))
				prettyPrintList(op, node);
			else {
				prettyPrint0(tree.getLeft());
				incrementIndent();
				nl();
				append(getOperatorDisplayName(tree.getOperator()));
				prettyPrint0(tree.getRight());
				decrementIndent();
			}
		} else
			append(Formatter.dump(node));
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

				nl();
				append(getOperatorDisplayName(op));

				if (!isLeftAssoc)
					prettyPrintList(op, tree.getRight());
				else
					prettyPrint0(tree.getRight());

				return;
			}
		}

		prettyPrint0(node);
	}

	private String getOperatorDisplayName(Operator op) {
		String name = op.getName().trim();
		return name.length() > 0 ? name + " " : name;
	}

	private int estimateLeafStringLengths(Node node) {
		int key = getKey(node);
		Integer length = lengthByIds.get(key);

		if (length == null) {
			int len;

			if (node instanceof Tree) {
				Tree tree = (Tree) node;
				int len0 = estimateLeafStringLengths(tree.getLeft());
				int len1 = estimateLeafStringLengths(tree.getRight());
				int opLength = tree.getOperator().getName().length();
				len = len0 + len1 + opLength; // Rough estimation
			} else
				len = Formatter.dump(node).length();

			length = len;
			lengthByIds.put(key, length);
		}

		return length;
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
