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
		return parse(in1, new Segment(0, in1.length()), 0);
	}

	private Node parse(String s, Segment segment, int fromOp) {
		return parseRaw(s, trim(s, segment), fromOp);
	}

	private Node parseRaw(String s, Segment segment, int fromOp) {
		return segment.start < segment.end ? parseRaw0(s, segment, fromOp) : Atom.NIL;
	}

	private Node parseRaw0(String s, Segment segment, int fromOp) {
		char first = Util.charAt(s, segment.start), last = Util.charAt(s, segment.end - 1);

		for (int i = fromOp; i < operators.length; i++) {
			Operator operator = operators[i];
			Segment ops = ParseUtil.searchPosition(s, segment, operator);

			if (ops == null)
				continue;

			Segment left = trim(s, new Segment(segment.start, ops.start));
			Segment right = trim(s, new Segment(ops.end, segment.end));
			int li, ri;

			if (operator == TermOp.BRACES) {
				if (right.start >= right.end || s.charAt(right.end - 1) != '}')
					continue;

				right = new Segment(right.start, right.end - 1);
				li = 0;
				ri = 0;
			} else {
				if (operator == TermOp.TUPLE_)
					if (isEmpty(left))
						return parseRaw(s, right, fromOp);
					else if (isEmpty(right))
						return parseRaw(s, left, fromOp);

				boolean isLeftAssoc = operator.getAssoc() == Assoc.LEFT;
				li = fromOp + (isLeftAssoc ? 0 : 1);
				ri = fromOp + (isLeftAssoc ? 1 : 0);
			}

			return Tree.of(operator, parse(s, left, li), parse(s, right, ri));
		}

		if (first == '(' && last == ')')
			return parseRaw(s, inner(segment), 0);
		if (first == '[' && last == ']')
			return Tree.of(TermOp.TUPLE_, Atom.of("[]"), parseRaw(s, inner(segment), 0));
		if (first == '`' && last == '`')
			return Tree.of(TermOp.TUPLE_, Atom.of("`"), parseRaw(s, inner(segment), 0));

		return terminalParser.parseTerminal(s);
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

	private boolean isEmpty(Segment segment) {
		return segment.start == segment.end;
	}

}
