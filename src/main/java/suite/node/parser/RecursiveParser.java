package suite.node.parser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.util.Context;
import suite.node.util.Singleton;
import suite.parser.CommentPreprocessor;
import suite.parser.IndentationPreprocessor;
import suite.parser.WhitespacePreprocessor;
import suite.util.FunUtil.Fun;
import suite.util.ParseUtil;
import suite.util.Util;

/**
 * Recursive-descent parser for operator-based languages.
 * 
 * @author ywsing
 */
public class RecursiveParser {

	private Operator operators[];
	private Set<Character> whitespaces = new HashSet<>(Arrays.asList('\t', '\r', '\n'));

	private Fun<String, String> commentPreprocessor;
	private Fun<String, String> indentPreprocessor;
	private Fun<String, String> whitespacePreprocessor;

	private TerminalParser terminalParser;

	public RecursiveParser(Operator operators[]) {
		this(Singleton.get().getGrandContext(), operators);
	}

	public RecursiveParser(Context context, Operator operators[]) {
		this.operators = operators;
		commentPreprocessor = new CommentPreprocessor(whitespaces);
		indentPreprocessor = new IndentationPreprocessor(operators);
		whitespacePreprocessor = new WhitespacePreprocessor(whitespaces);
		terminalParser = new TerminalParser(context);
	}

	public Node parse(String in) {
		in = commentPreprocessor.apply(in);
		in = indentPreprocessor.apply(in);
		in = whitespacePreprocessor.apply(in);
		return parseWithoutComments(in);
	}

	/**
	 * Parse without comments.
	 */
	private Node parseWithoutComments(String s) {
		return parseWithoutComments(s, 0);
	}

	private Node parseWithoutComments(String s, int fromOp) {
		return parseRawString(s.trim(), fromOp);
	}

	private Node parseRawString(String s, int fromOp) {
		return !s.isEmpty() ? parseRawString0(s, fromOp) : Atom.NIL;
	}

	private Node parseRawString0(String s, int fromOp) {
		char first = Util.charAt(s, 0), last = Util.charAt(s, -1);

		for (int i = fromOp; i < operators.length; i++) {
			Operator operator = operators[i];
			String lr[] = ParseUtil.search(s, operator);
			int li, ri;

			if (lr == null)
				continue;

			if (operator == TermOp.BRACES) {
				String right = lr[1].trim();

				if (Util.charAt(right, -1) != '}')
					continue;

				lr[1] = Util.substr(right, 0, -1);
				li = 0;
				ri = 0;
			} else {
				if (operator == TermOp.TUPLE_)
					if (Util.isBlank(lr[0]))
						return parseRawString(lr[1], fromOp);
					else if (Util.isBlank(lr[1]))
						return parseRawString(lr[0], fromOp);

				boolean isLeftAssoc = operator.getAssoc() == Assoc.LEFT;
				li = fromOp + (isLeftAssoc ? 0 : 1);
				ri = fromOp + (isLeftAssoc ? 1 : 0);
			}

			return Tree.create(operator, parseWithoutComments(lr[0], li), parseWithoutComments(lr[1], ri));
		}

		if (first == '(' && last == ')')
			return parseRawString(Util.substr(s, 1, -1), 0);
		if (first == '[' && last == ']')
			return Tree.create(TermOp.TUPLE_, Atom.create("[]"), parseRawString(Util.substr(s, 1, -1), 0));
		if (first == '`' && last == '`')
			return Tree.create(TermOp.TUPLE_, Atom.create("`"), parseRawString(" " + Util.substr(s, 1, -1) + " ", 0));

		return terminalParser.parseTerminal(s);
	}

}
