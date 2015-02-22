package suite.node.io;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import suite.lp.sewing.SewingGeneralizer;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.Operator.Assoc;
import suite.node.util.IdentityKey;
import suite.parser.CommentTransformer;
import suite.primitive.Chars;
import suite.streamlet.As;
import suite.streamlet.Read;
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

				if (node instanceof Dict)
					content = Read.from(((Dict) node).map.entrySet()) //
							.map(e -> graphize(e.getKey()) + ":" + graphize(e.getValue()) + ", ") //
							.collect(As.joined("dict(", ", ", ")"));
				else if ((tree = Tree.decompose(node)) != null) {
					int id0 = graphize(tree.getLeft());
					int id1 = graphize(tree.getRight());
					content = "tree(" + id0 + tree.getOperator().getName() + id1 + ")";
				} else if (node instanceof Tuple)
					content = Read.from(((Tuple) node).nodes) //
							.map(n -> graphize(n) + ", ") //
							.collect(As.joined("tuple(", ", ", ")"));
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
		if (node instanceof Atom)
			sb.append(quoteAtomIfRequired(((Atom) node).name));
		else if (node instanceof Data) {
			Object data = Data.get(node);
			if (data instanceof Chars)
				sb.append("Chars<" + quoteStringIfRequired(((Chars) data).toString()) + ">");
			else
				sb.append("Data<" + data.getClass().getSimpleName() + ">");
		} else if (node instanceof Dict) {
			sb.append("dict<");
			for (Entry<Node, Reference> entry : ((Dict) node).map.entrySet()) {
				format(entry.getKey(), 0);
				sb.append(":");
				format(entry.getValue(), 0);
				sb.append(",");
			}
			sb.append(">");
		} else if (node instanceof Int)
			sb.append(((Int) node).number);
		else if (node instanceof Reference)
			sb.append(SewingGeneralizer.variablePrefix + ((Reference) node).getId());
		else if (node instanceof Str)
			sb.append(quoteStringIfRequired(((Str) node).value));
		else if (node instanceof Tree) {
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
		} else if (node instanceof Tuple) {
			sb.append("tuple<");
			for (Node n : ((Tuple) node).nodes) {
				format(n, 0);
				sb.append(", ");
			}
			sb.append(">");
		} else
			sb.append(node.getClass().getSimpleName() + '@' + Integer.toHexString(node.hashCode()));
	}

	private void formatTree(Operator operator, Node left, Node right, int parentPrec) {
		boolean isParenthesesRequired = operator.getPrecedence() <= parentPrec;
		if (isParenthesesRequired)
			sb.append('(');
		formatTree(operator, left, right);
		if (isParenthesesRequired)
			sb.append(')');
	}

	private void formatTree(Operator operator, Node left, Node right) {
		int ourPrec = operator.getPrecedence();
		Assoc assoc = operator.getAssoc();

		format(left, ourPrec - (assoc == Assoc.LEFT ? 1 : 0));

		if (operator != TermOp.BRACES) {
			boolean isSpaceBefore = TermOp.isSpaceBefore(operator);
			boolean isSpaceAfter = TermOp.isSpaceAfter(operator);

			if (isSpaceBefore)
				sb.append(' ');
			sb.append(operator.getName());
			if (isSpaceAfter && right != Atom.NIL)
				sb.append(' ');
			if (!isSpaceAfter || right != Atom.NIL)
				format(right, ourPrec - (assoc == Assoc.RIGHT ? 1 : 0));
		} else {
			sb.append(" {");
			format(right, 0);
			sb.append("}");
		}
	}

	private String quoteAtomIfRequired(String s0) {
		String s1;
		if (isDump)
			if (!s0.isEmpty()) {
				boolean quote = false;

				for (char c : Util.chars(s0))
					quote |= !('0' <= c && c <= '9') //
							&& !('a' <= c && c <= 'z') //
							&& !('A' <= c && c <= 'Z') //
							&& c != '.' && c != '-' && c != '_' && c != '$' && c != '!';

				quote |= s0.contains(CommentTransformer.openGroupComment) //
						|| s0.contains(CommentTransformer.closeGroupComment) //
						|| s0.contains(CommentTransformer.openLineComment) //
						|| s0.contains(CommentTransformer.closeLineComment);

				quote |= ParseUtil.isInteger(s0);

				s1 = quote ? Escaper.escape(s0, '\'') : s0;
			} else
				s1 = "()";
		else
			s1 = s0;
		return s1;
	}

	private String quoteStringIfRequired(String s) {
		return isDump ? Escaper.escape(s, '"') : s;
	}

}
