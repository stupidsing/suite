package suite.node.parser;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.util.Context;
import suite.node.util.Singleton;
import suite.primitive.Chars;
import suite.primitive.CharsUtil;
import suite.text.Segment;
import suite.text.Transform;
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

	public Node parse(String in0) {
		String in1 = Transform.transform(TransformerFactory.create(operators), in0).t0;
		int p = 0;
		while (p < in1.length() && Character.isWhitespace(in1.charAt(p)))
			p++;
		return parse(To.chars(in1), 0);
	}

	private Node parse(Chars chars, int fromOp) {
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
						if (CharsUtil.isWhiespaces(left))
							return parse(right, fromOp);
						else if (CharsUtil.isWhiespaces(right))
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

	private Chars inner(Chars chars) {
		return Chars.of(chars.cs, chars.start + 1, chars.end - 1);
	}

}
