package suite.node.parser;

import java.util.HashMap;
import java.util.Map;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
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
			Chars unparse = textByKey.get(new IdentityKey(node));
			String s0;

			if (unparse != null) {
				int start = reverser.reverseBegin(unparse.start);
				int end = reverser.reverseEnd(unparse.end);
				s0 = in.substring(start, end);
			} else
				s0 = null;

			if (node instanceof Tree) {
				Tree tree = (Tree) node;
				Operator operator = tree.getOperator();
				Node left = tree.getLeft();
				Node right = tree.getRight();
				int ourPrec = operator.getPrecedence();
				Assoc assoc = operator.getAssoc();
				boolean isParenthesesRequired = ourPrec <= parentPrec;

				if (s0 != null) {
					String s1 = s0.trim();
					boolean hasParentheses = s1.startsWith("(") && s1.endsWith(")");
					if (!isParenthesesRequired || hasParentheses)
						sb.append(s0);
					else
						sb.append("(" + s1 + ")");
				} else {
					if (operator == TermOp.TUPLE_ && tree.getLeft() == Atom.of("[")) {
						sb.append("[");
						unparse0(sb, right, 0);
						sb.append("]");
					} else {
						if (isParenthesesRequired)
							sb.append('(');

						unparse0(sb, left, ourPrec - (assoc == Assoc.LEFT ? 1 : 0));

						if (operator != TermOp.BRACES) {
							sb.append(operator.getName());
							unparse0(sb, right, ourPrec - (assoc == Assoc.RIGHT ? 1 : 0));
						} else {
							sb.append(" {");
							unparse0(sb, right, 0);
							sb.append("}");
						}

						if (isParenthesesRequired)
							sb.append(')');
					}
				}
			} else if (s0 != null)
				sb.append(s0);
			else
				sb.append(Formatter.dump(node));
		}

		private Chars inner(Chars chars) {
			return Chars.of(chars.cs, chars.start + 1, chars.end - 1);
		}
	}

}
