package suite.node.io;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import suite.lp.doer.Generalizer;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Operator.Assoc;
import suite.parser.CommentPreprocessor;
import suite.util.ParseUtil;
import suite.util.Util;

public class Formatter {

	private boolean isDump;
	private Set<Integer> set = new HashSet<>();
	private StringBuilder sb = new StringBuilder();

	public Formatter(boolean isDump) {
		this.isDump = isDump;
	}

	public static String display(Node node) {
		return new Formatter(false).format(node);
	}

	public static String dump(Node node) {
		return new Formatter(true).format(node);
	}

	public static String treeize(Node node) {
		StringBuilder sb = new StringBuilder();
		treeize(node, sb, "");
		return sb.toString();
	}

	private static void treeize(Node node, StringBuilder sb, String indent) {
		Tree tree = Tree.decompose(node);

		if (tree != null) {
			String op = tree.getOperator().getName();
			op = Util.stringEquals(op, " ") ? "<>" : op.trim();
			String indent1 = indent + "  ";

			treeize(tree.getLeft(), sb, indent1);
			sb.append(indent + op + "\n");
			treeize(tree.getRight(), sb, indent1);
		} else
			sb.append(indent + dump(node) + "\n");
	}

	private String format(Node node) {
		format(node, 0);
		return sb.toString();
	}

	/**
	 * Converts a node to its string representation.
	 * 
	 * @param node
	 *            Node to be converted.
	 * @param parentPrec
	 *            Minimum operator precedence without adding parentheses.
	 */
	private void format(Node node, int parentPrec) {
		node = node.finalNode();
		Integer objectId = System.identityHashCode(node);

		// Avoids infinite recursion if object is recursive
		if (set.add(objectId)) {
			format0(node, parentPrec);
			set.remove(objectId);
		} else
			sb.append("<<recurse>>");
	}

	private void format0(Node node, int parentPrec) {
		if (node instanceof Int)
			sb.append(((Int) node).getNumber());
		else if (node instanceof Atom) {
			String s = ((Atom) node).getName();
			s = isDump ? quoteAtomIfRequired(s) : s;
			sb.append(s);
		} else if (node instanceof Str) {
			String s = ((Str) node).getValue();
			s = isDump ? Escaper.escape(s, '"') : s;
			sb.append(s);
		} else if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Operator operator = tree.getOperator();
			Node left = tree.getLeft();
			Node right = tree.getRight();

			if (operator == TermOp.TUPLE_ && left == Atom.create("[")) {
				sb.append("[");
				format(right);
				sb.append("]");
			} else
				formatTree(operator, left, right, parentPrec);
		} else if (node instanceof Reference)
			sb.append(Generalizer.variablePrefix + ((Reference) node).getId());
		else
			sb.append(node.getClass().getSimpleName() + '@' + Integer.toHexString(node.hashCode()));
	}

	private void formatTree(Operator operator, Node left, Node right, int parentPrec) {
		int ourPrec = operator.getPrecedence();
		Assoc assoc = operator.getAssoc();
		boolean isParenthesesRequired = ourPrec <= parentPrec;

		if (isParenthesesRequired)
			sb.append('(');

		format(left, ourPrec - (assoc == Assoc.LEFT ? 1 : 0));

		if (operator != TermOp.BRACES) {
			if (Arrays.asList(TermOp.NEXT__).contains(operator))
				sb.append(' ');

			String name = operator.getName();
			sb.append(name);

			if (!Arrays.asList(TermOp.NEXT__, TermOp.AND___, TermOp.OR____).contains(operator) || right != Atom.NIL) {
				if (Arrays.asList(TermOp.NEXT__, TermOp.AND___, TermOp.OR____).contains(operator))
					sb.append(' ');

				format(right, ourPrec - (assoc == Assoc.RIGHT ? 1 : 0));
			} // a, () suppressed as a,
		} else {
			sb.append(" {");
			format(right, 0);
			sb.append("}");
		}

		if (isParenthesesRequired)
			sb.append(')');
	}

	public String quoteAtomIfRequired(String s) {
		if (!s.isEmpty()) {
			boolean quote = false;

			for (char c : s.toCharArray())
				quote |= !('0' <= c && c <= '9') //
						&& !('a' <= c && c <= 'z') //
						&& !('A' <= c && c <= 'Z') //
						&& c != '.' && c != '-' && c != '_' && c != '$' && c != '!';

			quote |= s.contains(CommentPreprocessor.closeGroupComment) //
					|| s.contains(CommentPreprocessor.openGroupComment) //
					|| s.contains(CommentPreprocessor.closeLineComment) //
					|| s.contains(CommentPreprocessor.openLineComment);

			quote |= ParseUtil.isInteger(s);

			if (quote)
				s = Escaper.escape(s, '\'');
		} else
			s = "()";
		return s;
	}

}
