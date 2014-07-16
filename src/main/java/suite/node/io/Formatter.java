package suite.node.io;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import suite.lp.doer.Generalizer;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.Operator.Assoc;
import suite.node.util.IdentityKey;
import suite.parser.CommentPreprocessor;
import suite.util.ParseUtil;
import suite.util.Util;

public class Formatter {

	private boolean isDump;
	private Set<Integer> set = new HashSet<>();
	private StringBuilder sb = new StringBuilder();

	private static class Graphizer {
		private int count;
		private Map<IdentityKey, Integer> ids = new HashMap<>();
		private StringBuilder sb = new StringBuilder();

		private int graphize(Node node) {
			IdentityKey key = new IdentityKey(node.finalNode());
			Integer id = ids.get(key);
			Tree tree;

			if (id == null) {
				ids.put(key, id = count++);
				String content;

				if ((tree = Tree.decompose(node)) != null) {
					int id0 = graphize(tree.getLeft());
					int id1 = graphize(tree.getRight());
					content = "tree(" + id0 + tree.getOperator().getName() + id1 + ")";
				} else if (node instanceof Tuple)
					content = ((Tuple) node).getNodes().stream() //
							.map(n -> graphize(n) + ", ").collect(Collectors.joining(", ", "tuple(", ")"));
				else
					content = dump(node);

				sb.append(id + " = " + content + "\n");
			}

			return id;
		}
	}

	private static class Treeizer {
		private StringBuilder sb = new StringBuilder();

		private void treeize(Node node, String indent) {
			Tree tree = Tree.decompose(node);

			if (tree != null) {
				String op = tree.getOperator().getName();
				op = Util.stringEquals(op, " ") ? "<>" : op.trim();
				String indent1 = indent + "  ";

				treeize(tree.getLeft(), indent1);
				sb.append(indent + op + "\n");
				treeize(tree.getRight(), indent1);
			} else
				sb.append(indent + dump(node) + "\n");
		}
	}

	public Formatter(boolean isDump) {
		this.isDump = isDump;
	}

	public static String display(Node node) {
		return new Formatter(false).format(node);
	}

	public static String dump(Node node) {
		return new Formatter(true).format(node);
	}

	public static String graphize(Node node) {
		Graphizer graphizer = new Graphizer();
		int fn = graphizer.graphize(node);
		return graphizer.sb.toString() + "return(" + fn + ")\n";
	}

	public static String treeize(Node node) {
		Treeizer treeize = new Treeizer();
		treeize.treeize(node, "");
		return treeize.sb.toString();
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
		if (node instanceof Atom) {
			String s = ((Atom) node).getName();
			s = isDump ? quoteAtomIfRequired(s) : s;
			sb.append(s);
		} else if (node instanceof Int)
			sb.append(((Int) node).getNumber());
		else if (node instanceof Reference)
			sb.append(Generalizer.variablePrefix + ((Reference) node).getId());
		else if (node instanceof Str) {
			String s = ((Str) node).getValue();
			s = isDump ? Escaper.escape(s, '"') : s;
			sb.append(s);
		} else if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Operator operator = tree.getOperator();
			Node left = tree.getLeft();
			Node right = tree.getRight();

			if (operator == TermOp.TUPLE_ && left == Atom.of("[")) {
				sb.append("[");
				format(right);
				sb.append("]");
			} else
				formatTree(operator, left, right, parentPrec);
		} else
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
