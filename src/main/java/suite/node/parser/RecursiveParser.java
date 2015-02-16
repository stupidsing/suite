package suite.node.parser;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.util.Context;
import suite.node.util.Singleton;
import suite.text.Transform;
import suite.util.Pair;
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
		String in1 = Transform.transform(SuiteParser.createTransformer(operators), in0).t0;
		return parse(in1, 0);
	}

	private Node parse(String s, int fromOp) {
		return parseRaw(s.trim(), fromOp);
	}

	private Node parseRaw(String s, int fromOp) {
		return !s.isEmpty() ? parseRaw0(s, fromOp) : Atom.NIL;
	}

	private Node parseRaw0(String s, int fromOp) {
		char first = Util.charAt(s, 0), last = Util.charAt(s, -1);

		for (int i = fromOp; i < operators.length; i++) {
			Operator operator = operators[i];
			Pair<String, String> pair = ParseUtil.search(s, operator);
			int li, ri;

			if (pair == null)
				continue;

			if (operator == TermOp.BRACES) {
				String right = pair.t1.trim();

				if (Util.charAt(right, -1) != '}')
					continue;

				pair.t1 = Util.substr(right, 0, -1);
				li = 0;
				ri = 0;
			} else {
				if (operator == TermOp.TUPLE_)
					if (Util.isBlank(pair.t0))
						return parseRaw(pair.t1, fromOp);
					else if (Util.isBlank(pair.t1))
						return parseRaw(pair.t0, fromOp);

				boolean isLeftAssoc = operator.getAssoc() == Assoc.LEFT;
				li = fromOp + (isLeftAssoc ? 0 : 1);
				ri = fromOp + (isLeftAssoc ? 1 : 0);
			}

			return Tree.of(operator, parse(pair.t0, li), parse(pair.t1, ri));
		}

		if (first == '(' && last == ')')
			return parseRaw(Util.substr(s, 1, -1), 0);
		if (first == '[' && last == ']')
			return Tree.of(TermOp.TUPLE_, Atom.of("[]"), parseRaw(Util.substr(s, 1, -1), 0));
		if (first == '`' && last == '`')
			return Tree.of(TermOp.TUPLE_, Atom.of("`"), parseRaw(Util.substr(s, 1, -1), 0));

		return terminalParser.parseTerminal(s);
	}

}
