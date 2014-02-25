package suite.node.io;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermParser.TermOp;
import suite.node.util.Context;
import suite.node.util.Singleton;
import suite.util.FunUtil.Fun;
import suite.util.LogUtil;
import suite.util.ParseUtil;
import suite.util.Util;

public class Parser {

	private Context localContext;
	private Operator operators[];
	private Set<Character> whitespaces = new HashSet<>(Arrays.asList('\t', '\r', '\n'));

	private Fun<String, String> commentProcessor;
	private Fun<String, String> indentProcessor;
	private Fun<String, String> whitespaceProcessor;

	public Parser(Operator operators[]) {
		this(Singleton.get().getGrandContext(), operators);
	}

	public Parser(Context context, Operator operators[]) {
		localContext = context;
		this.operators = operators;
		commentProcessor = new CommentProcessor(whitespaces);
		indentProcessor = new IndentationProcessor(operators);
		whitespaceProcessor = new WhitespaceProcessor(whitespaces);
	}

	public Node parse(String in) {
		in = commentProcessor.apply(in);
		in = indentProcessor.apply(in);
		in = whitespaceProcessor.apply(in);
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

		if (ParseUtil.isInteger(s))
			return Int.create(Integer.parseInt(s));
		if (s.startsWith("+x"))
			return Int.create(Integer.parseInt(s.substring(2), 16));
		if (s.startsWith("+'") && s.endsWith("'") && s.length() == 4)
			return Int.create(s.charAt(2));

		if (first == '"' && last == '"')
			return new Str(Escaper.unescape(Util.substr(s, 1, -1), "\""));

		if (first == '\'' && last == '\'')
			s = Escaper.unescape(Util.substr(s, 1, -1), "'");
		else {
			s = s.trim(); // Trim unquoted atoms
			if (!ParseUtil.isParseable(s))
				LogUtil.info("Suspicious input when parsing " + s);
		}

		return Atom.create(localContext, s);
	}

}
