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
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator.Assoc;
import suite.node.util.Context;
import suite.node.util.Singleton;
import suite.util.FunUtil.Fun;
import suite.util.Util;

/**
 * TODO handle `` for operators containing spaces
 * 
 * @author ywsing
 */
public class IterativeParser {

	private Set<Character> whitespaces = new HashSet<>(Arrays.asList('\t', '\r', '\n', ' '));
	private int maxOperatorLength;
	private Map<String, Operator> operatorsByName = new HashMap<>();

	private Fun<String, String> commentProcessor;
	private Fun<String, String> indentProcessor;
	private Fun<String, String> whitespaceProcessor;

	private TerminalParser terminalParser;

	public IterativeParser(Operator operators[]) {
		this(Singleton.get().getGrandContext(), operators);
	}

	private IterativeParser(Context context, Operator operators[]) {
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

		terminalParser = new TerminalParser(context);
	}

	public Node parse(String in) {
		in = commentProcessor.apply(in);
		in = indentProcessor.apply(in);
		in = whitespaceProcessor.apply(in);
		return new Parse(in).parse();
	}

	private enum LexType {
		CHAR_, ID___, OPER_, SPACE, STR__
	}

	private class Token {
		private LexType type;
		private Operator operator;
		private String data;

		private Token(LexType type, Operator operator) {
			this.type = type;
			this.operator = operator;
		}
	}

	private class Lex {
		private String in;
		private int pos = 0;

		private Lex(String in) {
			this.in = in;
		}

		private Token lex() {
			int start = pos;

			if (pos < in.length()) {
				Token token = detect();
				LexType type = token.type;

				if (type == LexType.ID___ || type == LexType.SPACE)
					while (pos < in.length() && detect().type == type)
						pos++;
				else if (type == LexType.CHAR_)
					pos++;
				else if (type == LexType.OPER_)
					pos += token.operator.getName().length();
				else if (type == LexType.STR__) {
					char quote = in.charAt(pos);
					while (pos < in.length() && in.charAt(pos) == quote) {
						pos++;
						while (pos < in.length() && in.charAt(pos) != quote)
							pos++;
						pos++;
					}
				}

				if (type != LexType.SPACE) {
					token.data = in.substring(start, pos);
					return token;
				} else
					return lex();
			} else
				return null;
		}

		private Token detect() {
			LexType type;
			Operator operator = detectOperator();

			if (pos < in.length()) {
				char ch = in.charAt(pos);

				if (operator != null)
					type = LexType.OPER_;
				else if (ch == '(' || ch == '[' || ch == '{' //
						|| ch == ')' || ch == ']' || ch == '}' //
						|| ch == '`')
					type = LexType.CHAR_;
				else if (isWhitespace(ch))
					type = LexType.SPACE;
				else if (ch == '\'' || ch == '"')
					type = LexType.STR__;
				else
					type = LexType.ID___;
			} else
				type = null;

			return new Token(type, operator);
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
	}

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

	private class Parse {
		private String in;
		private Deque<Section> stack = new ArrayDeque<>();

		private Parse(String in) {
			this.in = in;
		}

		private Node parse() {
			Lex lex = new Lex(in);
			stack.push(new Section(' '));
			Token token;

			while ((token = lex.lex()) != null) {
				Operator operator = token.operator;
				String data = token.data;
				char ch = data.charAt(0);

				if (operator != null) {
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
				else if (Util.isNotBlank(data))
					add(parse0(data));
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
			Tree.forceSetRight(section.last(), node);
			section.isDanglingRight = false;
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
		return terminalParser.parseTerminal(in);
	}

	private boolean isWhitespace(char ch) {
		return whitespaces.contains(ch);
	}

}
