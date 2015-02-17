package suite.node.parser;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.util.Context;
import suite.node.util.Singleton;
import suite.text.Segment;
import suite.text.Transform;
import suite.util.ParseUtil;
import suite.util.Util;

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
		return parse(in1, new Segment(p, in1.length()), 0);
	}

	private Node parse(String s, Segment segment, int fromOp) {
		Segment segment1 = trim(s, segment);

		if (segment1.start < segment1.end) {
			char first = Util.charAt(s, segment1.start);
			char last = Util.charAt(s, segment1.end - 1);

			for (int i = fromOp; i < operators.length; i++) {
				Operator operator = operators[i];
				Segment ops = ParseUtil.searchPosition(s, segment, operator);

				if (ops == null)
					continue;

				Segment left = new Segment(segment.start, ops.start);
				Segment right = new Segment(ops.end, segment.end);
				int li, ri;

				if (operator == TermOp.BRACES) {
					if (last != '}')
						continue;

					right = new Segment(right.start, segment1.end - 1);
					li = 0;
					ri = 0;
				} else {
					if (operator == TermOp.TUPLE_)
						if (left.start == left.end)
							return parse(s, right, fromOp);
						else if (right.start == right.end)
							return parse(s, left, fromOp);

					boolean isLeftAssoc = operator.getAssoc() == Assoc.LEFT;
					li = fromOp + (isLeftAssoc ? 0 : 1);
					ri = fromOp + (isLeftAssoc ? 1 : 0);
				}

				return Tree.of(operator, parse(s, left, li), parse(s, right, ri));
			}

			if (first == '(' && last == ')')
				return parse(s, inner(segment1), 0);
			if (first == '[' && last == ']')
				return Tree.of(TermOp.TUPLE_, Atom.of("[]"), parse(s, inner(segment1), 0));
			if (first == '`' && last == '`')
				return Tree.of(TermOp.TUPLE_, Atom.of("`"), parse(s, inner(segment1), 0));

			return terminalParser.parseTerminal(s.substring(segment1.start, segment1.end));
		} else
			return Atom.NIL;
	}

	private Segment trim(String s, Segment segment0) {
		int start = segment0.start, end = segment0.end;
		while (start < end && Character.isWhitespace(s.charAt(start)))
			start++;
		while (start < end && Character.isWhitespace(s.charAt(end - 1)))
			end--;
		return new Segment(start, end);
	}

	private Segment inner(Segment segment) {
		return new Segment(segment.start + 1, segment.end - 1);
	}

}
