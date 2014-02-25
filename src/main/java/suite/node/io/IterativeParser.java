package suite.node.io;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

/**
 * TODO handle `` for operators containing spaces
 * 
 * @author ywsing
 */
public class IterativeParser {

	private Context localContext;
	private Set<Character> whitespaces = new HashSet<>(Arrays.asList('\t', '\r', '\n', ' '));
	private int maxOperatorLength;
	private Map<String, Operator> operatorsByName = new HashMap<>();

	private Fun<String, String> commentProcessor;
	private Fun<String, String> indentProcessor;
	private Fun<String, String> whitespaceProcessor;

	private class Section {
		private char kind;
		private List<Tree> list = new ArrayList<>(Arrays.asList(Tree.create(null, null, Atom.NIL)));
		private boolean isDanglingRight = true;

		public Section(char kind) {
			this.kind = kind;
		}

		private Tree first() {
			return list.get(0);
		}

		private Tree last() {
			return Util.last(list);
		}

		private void push(Tree tree) {
			list.add(tree);
			isDanglingRight = true;
		}
	}

	public IterativeParser(Operator operators[]) {
		this(Singleton.get().getGrandContext(), operators);
	}

	public IterativeParser(Context context, Operator operators[]) {
		localContext = context;
		maxOperatorLength = 0;

		for (Operator operator : operators)
			if (operator != TermOp.TUPLE_) {
				String name = operator.getName();
				maxOperatorLength = Math.max(maxOperatorLength, name.length());
				operatorsByName.put(name, operator);
			}

		commentProcessor = new CommentProcessor(whitespaces);
		indentProcessor = new IndentationProcessor(operators);
		whitespaceProcessor = new WhitespaceProcessor(whitespaces);
	}

	public Node parse(String in) {
		in = commentProcessor.apply(in);
		in = indentProcessor.apply(in);
		in = whitespaceProcessor.apply(in);
		return new Parse(in).parse();
	}

	private enum TokenKind {
		CHAR_, ID___, OPER_, SPACE, STR__
	}

	private class Parse {
		private String in;
		private Deque<Section> stack = new ArrayDeque<>();
		private int pos = 0;

		private Parse(String in) {
			this.in = in;
		}

		private String lex() {
			int start = pos;

			if (pos < in.length()) {
				TokenKind kind = detect();

				if (kind == TokenKind.CHAR_)
					pos++;
				else if (kind == TokenKind.ID___ || kind == TokenKind.SPACE)
					while (pos < in.length() && detect() == kind)
						pos++;
				else if (kind == TokenKind.OPER_)
					pos += detectOperator().getName().length();
				else if (kind == TokenKind.STR__) {
					char quote = in.charAt(pos);
					while (pos < in.length() && in.charAt(pos) == quote) {
						pos++;
						while (pos < in.length() && in.charAt(pos) != quote)
							pos++;
						pos++;
					}
				}

				return kind != TokenKind.SPACE ? in.substring(start, pos) : lex();
			} else
				return null;
		}

		private TokenKind detect() {
			if (pos < in.length()) {
				char ch = in.charAt(pos);

				if (ch == '(' || ch == '[' || ch == '{' //
						|| ch == ')' || ch == ']' || ch == '}' //
						|| ch == '`')
					return TokenKind.CHAR_;
				else if (detectOperator() != null)
					return TokenKind.OPER_;
				else if (isWhitespace(ch))
					return TokenKind.SPACE;
				else if (ch == '\'' || ch == '"')
					return TokenKind.STR__;
				else
					return TokenKind.ID___;
			} else
				return null;
		}

		private Operator detectOperator() {
			for (int length = maxOperatorLength; length > 0; length--)
				if (pos + length < in.length()) {
					Operator op = operatorsByName.get(in.substring(pos, pos + length));
					if (op != null)
						return op;
				}

			return null;
		}

		private Node parse() {
			stack.push(new Section(' '));
			String token;

			while ((token = lex()) != null) {
				char ch = token.charAt(0);
				Operator operator;

				if ((operator = operatorsByName.get(token)) != null) {
					addOperator(operator);
					if (operator == TermOp.BRACES)
						stack.push(new Section('{'));
				} else if (ch == '(' || ch == '[' || ch == '{')
					stack.push(new Section(ch));
				else if (ch == ')' || ch == ']' || ch == '}') {
					Section section = stack.pop();

					if (section.kind == '(' && ch == ')' //
							|| section.kind == '[' && ch == ']' //
							|| section.kind == '{' && ch == '}') {
						Node node = section.first().getRight();
						if (ch == ']')
							node = Tree.create(TermOp.TUPLE_, Atom.create("["), node);
						add(node);
					} else
						throw new RuntimeException("Cannot parse " + in);
				} else if (ch == '`')
					if (stack.peek().kind == ch) {
						Node node = stack.pop().first().getRight();
						node = Tree.create(TermOp.TUPLE_, Atom.create("`"), node);
						add(node);
					} else
						stack.push(new Section(ch));
				else if (Util.isNotBlank(token))
					add(parse0(token));
			}

			if (stack.size() == 1)
				return stack.pop().first().getRight();
			else
				throw new RuntimeException("Cannot parse " + in);
		}

		private void add(Node node) {
			Section section = stack.peek();
			if (!section.isDanglingRight)
				addOperator(TermOp.TUPLE_);
			else
				section.isDanglingRight = false;
			Tree.forceSetRight(section.last(), node);
		}

		private void addOperator(Operator operator) {
			Section section = stack.peek();
			List<Tree> list = section.list;
			int listPos = list.size() - 1;
			int prec0 = operator.getPrecedence();
			Operator op;
			Tree tree;

			while ((op = (tree = list.get(listPos)).getOperator()) != null) {
				int prec1 = op.getPrecedence();
				if (prec0 < prec1 || operator.getAssoc() == Assoc.LEFT && prec0 == prec1)
					listPos--;
				else
					break;
			}

			Tree tree1 = Tree.create(operator, tree.getRight(), Atom.NIL);
			Tree.forceSetRight(tree, tree1);
			list.subList(listPos + 1, list.size()).clear();
			section.push(tree1);
		}
	}

	private Node parse0(String in) {
		char first = Util.charAt(in, 0), last = Util.charAt(in, -1);

		if (ParseUtil.isInteger(in))
			return Int.create(Integer.parseInt(in));
		if (in.startsWith("+x"))
			return Int.create(Integer.parseInt(in.substring(2), 16));
		if (in.startsWith("+'") && in.endsWith("'") && in.length() == 4)
			return Int.create(in.charAt(2));

		if (first == '"' && last == '"')
			return new Str(Escaper.unescape(Util.substr(in, 1, -1), "\""));

		if (first == '\'' && last == '\'')
			in = Escaper.unescape(Util.substr(in, 1, -1), "'");
		else {
			in = in.trim(); // Trim unquoted atoms
			if (!ParseUtil.isParseable(in))
				LogUtil.info("Suspicious input when parsing " + in);
		}

		return Atom.create(localContext, in);
	}

	private boolean isWhitespace(char ch) {
		return whitespaces.contains(ch);
	}

}
