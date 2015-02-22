package suite.node.parser;

import java.util.HashMap;
import java.util.Map;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.CustomStyles;
import suite.node.io.Formatter;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.util.Context;
import suite.node.util.IdentityKey;
import suite.node.util.Singleton;
import suite.primitive.Chars;
import suite.primitive.CharsUtil;
import suite.text.Segment;
import suite.text.Transform;
import suite.text.Transform.Reverser;
import suite.util.FunUtil.Fun;
import suite.util.Pair;
import suite.util.ParseUtil;
import suite.util.To;

/**
 * Recursive-descent parser for operator-based languages.
 *
 * @author ywsing
 */
public class RecursiveParser {

	private Operator operators[];
	private TerminalParser terminalParser;

	public RecursiveParser(Operator operators[]) {
		this(Singleton.get().getGrandContext(), operators);
	}

	public RecursiveParser(Context context, Operator operators[]) {
		this.operators = operators;
		terminalParser = new TerminalParser(context);
	}

	public Node parse(String in) {
		return analyze(in).parsed;
	}

	public String refactor(String in, Fun<Node, Node> fun) {
		RecursiveParse rp = analyze(in);
		return rp.unparse(fun.apply(rp.parsed));
	}

	public RecursiveParse analyze(String in) {
		return new RecursiveParse(in);
	}

	public class RecursiveParse {
		public final Node parsed;
		private String in;
		private Reverser reverser;
		private Map<IdentityKey, Chars> textByKey = new HashMap<>();

		private RecursiveParse(String in0) {
			this.in = in0;
			Pair<String, Reverser> pair = Transform.transform(TransformerFactory.create(operators), in0);
			String in1 = pair.t0;
			reverser = pair.t1;

			parsed = parse(To.chars(in1), 0);
		}

		public String unparse(Node node) {
			StringBuilder sb = new StringBuilder();
			unparse0(sb, node, 0);
			return sb.toString();
		}

		private Node parse(Chars chars, int fromOp) {
			Node node = parse0(chars, fromOp);
			textByKey.put(new IdentityKey(node), chars);
			return node;
		}

		private Node parse0(Chars chars, int fromOp) {
			Chars chars1 = CharsUtil.trim(chars);

			if (chars1.size() > 0) {
				char first = chars.get(0);
				char last = chars.get(-1);

				for (int i = fromOp; i < operators.length; i++) {
					Operator operator = operators[i];
					Segment ops = ParseUtil.searchPosition(chars.cs, new Segment(chars.start, chars.end), operator);

					if (ops == null)
						continue;

					Chars left = Chars.of(chars.cs, chars.start, ops.start);
					Chars right = Chars.of(chars.cs, ops.end, chars.end);
					int li, ri;

					if (operator == TermOp.BRACES) {
						if (last != '}')
							continue;

						right = Chars.of(chars.cs, ops.end, chars1.end - 1);
						li = 0;
						ri = 0;
					} else {
						if (operator == TermOp.TUPLE_)
							if (CharsUtil.isWhitespaces(left))
								return parse(right, fromOp);
							else if (CharsUtil.isWhitespaces(right))
								return parse(left, fromOp);

						boolean isLeftAssoc = operator.getAssoc() == Assoc.LEFT;
						li = fromOp + (isLeftAssoc ? 0 : 1);
						ri = fromOp + (isLeftAssoc ? 1 : 0);
					}

					return Tree.of(operator, parse(left, li), parse(right, ri));
				}

				if (first == '(' && last == ')')
					return parse(inner(chars1), 0);
				if (first == '[' && last == ']')
					return Tree.of(TermOp.TUPLE_, Atom.of("[]"), parse(inner(chars1), 0));
				if (first == '`' && last == '`')
					return Tree.of(TermOp.TUPLE_, Atom.of("`"), parse(inner(chars1), 0));

				return terminalParser.parseTerminal(Chars.of(chars.cs, chars1.start, chars1.end).toString());
			} else
				return Atom.NIL;
		}

		private void unparse0(StringBuilder sb, Node node, int parentPrec) {
			String text;
			if (node instanceof Tree)
				formatTree(sb, (Tree) node, parentPrec);
			else if ((text = getTextByKey(node)) != null)
				sb.append(text);
			else
				sb.append(Formatter.dump(node));
		}

		private void formatTree(StringBuilder sb, Tree tree, int parentPrec) {
			Node m[];
			if ((m = CustomStyles.bracketMatcher.apply(tree)) != null) {
				sb.append("[");
				unparse0(sb, m[0], 0);
				sb.append("]");
			} else
				formatTree0(sb, tree, parentPrec);
		}

		private void formatTree0(StringBuilder sb, Tree tree, int parentPrec) {
			if (tree != null && tree.getOperator().getPrecedence() <= parentPrec) {
				sb.append("(");
				formatTree(sb, tree);
				sb.append(")");
			} else
				formatTree(sb, tree);
		}

		private void formatTree(StringBuilder sb, Tree tree) {
			String text = getTextByKey(tree);
			Node[] m;
			if (text != null)
				sb.append(text);
			else if ((m = CustomStyles.braceMatcher.apply(tree)) != null) {
				unparse0(sb, m[0], TermOp.getLeftPrec(TermOp.BRACES));
				sb.append(" {");
				unparse0(sb, m[1], 0);
				sb.append("}");
			} else
				formatTree0(sb, tree);
		}

		private void formatTree0(StringBuilder sb, Tree tree) {
			Operator operator = tree.getOperator();
			Node left = tree.getLeft();
			Node right = tree.getRight();
			boolean isSpaceBefore = TermOp.isSpaceBefore(operator);
			boolean isSpaceAfter = TermOp.isSpaceAfter(operator);

			unparse0(sb, left, TermOp.getLeftPrec(operator));
			if (isSpaceBefore)
				sb.append(' ');
			sb.append(operator.getName());
			if (isSpaceAfter && right != Atom.NIL)
				sb.append(' ');
			if (!isSpaceAfter || right != Atom.NIL)
				unparse0(sb, right, TermOp.getRightPrec(operator));
			// a, () suppressed as a,
		}

		private String getTextByKey(Node node) {
			Chars unparse = textByKey.get(new IdentityKey(node));
			String text;

			if (unparse != null) {
				int start = reverser.reverseBegin(unparse.start);
				int end = reverser.reverseEnd(unparse.end);
				text = in.substring(start, end);
			} else
				text = null;
			return text;
		}

		private Chars inner(Chars chars) {
			return Chars.of(chars.cs, chars.start + 1, chars.end - 1);
		}
	}

}
