package suite.node.io;

import java.util.HashSet;
import java.util.Set;

import primal.String_;
import suite.lp.doer.ProverConstant;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.parser.CommentPreprocessor;
import suite.primitive.Chars;
import suite.streamlet.Read;

/**
 * Formats a node for human-readable purpose (display), or for
 * computer-parseable purpose (dump).
 *
 * Data, Dict and Tuple, even if they are dumped, cannot be parsed back into
 * their corresponding nodes.
 *
 * @author ywsing
 */
public class Formatter {

	private boolean isDump;
	private Set<Integer> set = new HashSet<>();
	private StringBuilder sb = new StringBuilder();

	/**
	 * Converts a node into an ugly tree representation. Children are dumped
	 * line-by-line with indentation.
	 */
	private static class Treeizer {
		private StringBuilder sb = new StringBuilder();

		private void treeize(Node node, String indent) {
			var indent1 = indent + "  ";

			new SwitchNode<Node>(node //
			).doIf(Dict.class, n -> {
				for (var e : Dict.m(node).entrySet()) {
					sb.append(indent + "d:" + dump(e.getKey()) + "\n");
					treeize(e.getValue().finalNode(), indent1);
				}
			}).doIf(Tree.class, tree -> {
				var op = tree.getOperator().name_();
				op = String_.equals(op, " ") ? "<>" : op.trim();

				treeize(tree.getLeft(), indent1);
				sb.append(indent + op + "\n");
				treeize(tree.getRight(), indent1);
			}).doIf(Tuple.class, n -> {
				for (var child : Tuple.t(node))
					sb.append(indent + "t:" + dump(child) + "\n");
			}).doIf(Node.class, n -> {
				sb.append(indent + dump(node) + "\n");
			}).nonNullResult();
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
		var grapher = new Grapher();
		grapher.graph(node);
		return grapher.toString();
	}

	public static String treeize(Node node) {
		var treeize = new Treeizer();
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
	private void format(Node node0, int parentPrec) {
		var node = node0.finalNode();
		var objectId = System.identityHashCode(node);

		// avoids infinite recursion if object is recursive
		if (set.add(objectId)) {
			format_(node, parentPrec);
			set.remove(objectId);
		} else
			sb.append("<<recurse>>");
	}

	private void format_(Node node, int parentPrec) {
		new SwitchNode<Node>(node //
		).doIf(Atom.class, n -> {
			sb.append(quoteAtomIfRequired(n.name));
		}).doIf(Data.class, n -> {
			var data = n.data;
			if (data instanceof Chars)
				sb.append("Chars<" + quoteStringIfRequired(data.toString()) + ">");
			else if (data instanceof Node)
				sb.append("Data<" + data.toString() + ">");
			else
				sb.append("Data<" + data.getClass().getSimpleName() + ">");
		}).doIf(Dict.class, n -> {
			sb.append("dict<");
			for (var e : n.getMap().entrySet()) {
				format(e.getKey(), TermOp.getLeftPrec(TermOp.AND___));
				sb.append(":");
				format(e.getValue(), TermOp.getLeftPrec(TermOp.AND___));
				sb.append(",");
			}
			sb.append(">");
		}).doIf(Int.class, n -> {
			sb.append(n.number);
		}).doIf(Reference.class, n -> {
			sb.append(n.name());
		}).doIf(Str.class, n -> {
			sb.append(quoteStringIfRequired(n.value));
		}).doIf(Tree.class, n -> {
			formatTree(n, parentPrec);
		}).doIf(Tuple.class, n -> {
			sb.append("tuple<");
			for (var n_ : n.nodes) {
				format(n_, TermOp.getLeftPrec(TermOp.AND___));
				sb.append(", ");
			}
			sb.append(">");
		}).doIf(Node.class, n -> {
			sb.append(n.getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(n)));
		}).nonNullResult();
	}

	private void formatTree(Tree tree, int parentPrec) {
		if (tree.getOperator() == TermOp.TUPLE_ && tree.getLeft() == Atom.of("[")) {
			sb.append("[");
			format(tree.getRight(), 0);
			sb.append("]");
		} else
			formatTree_(tree, parentPrec);
	}

	private void formatTree_(Tree tree, int parentPrec) {
		if (tree != null && tree.getOperator().precedence() <= parentPrec) {
			sb.append("(");
			formatTree(tree);
			sb.append(")");
		} else
			formatTree(tree);
	}

	private void formatTree(Tree tree) {
		Node[] m;
		if ((m = CustomStyles.patBrace.match(tree)) != null) {
			format(m[0], TermOp.getLeftPrec(TermOp.BRACES));
			sb.append("_{");
			format(m[1], 0);
			sb.append("}");
		} else
			formatTree_(tree);
	}

	private void formatTree_(Tree tree) {
		var operator = tree.getOperator();
		var left = tree.getLeft();
		var right = tree.getRight();
		var isSpaceBefore = TermOp.isSpaceBefore(operator);
		var isSpaceAfter = TermOp.isSpaceAfter(operator);

		format(left, TermOp.getLeftPrec(operator));
		if (isSpaceBefore)
			sb.append(' ');
		sb.append(operator.name_());
		if (isSpaceAfter && right != Atom.NIL)
			sb.append(' ');
		if (!isSpaceAfter || right != Atom.NIL)
			format(right, TermOp.getRightPrec(operator));
		// a, () suppressed as a,
	}

	private String quoteAtomIfRequired(String s0) {
		String s1;
		if (isDump)
			if (!s0.isEmpty()) {
				var quote = false;

				quote |= s0.startsWith(ProverConstant.variablePrefix) //
						|| s0.startsWith(ProverConstant.wildcardPrefix);

				for (var c : Read.chars(s0))
					quote |= !('0' <= c && c <= '9') //
							&& !('a' <= c && c <= 'z') //
							&& !('A' <= c && c <= 'Z') //
							&& c != '.' && c != '-' && c != '_' && c != '$' && c != '!';

				quote |= s0.contains(CommentPreprocessor.openGroupComment) //
						|| s0.contains(CommentPreprocessor.closeGroupComment) //
						|| s0.contains(CommentPreprocessor.openLineComment) //
						|| s0.contains(CommentPreprocessor.closeLineComment);

				quote |= String_.isInteger(s0);

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
