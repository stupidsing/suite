package suite.node.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.util.Context;
import suite.node.util.Singleton;
import suite.text.Transform;
import suite.util.CommandUtil;
import suite.util.Pair;
import suite.util.Util;

/**
 * Non-recursive, performance-improved parser for operator-based languages.
 *
 * @author ywsing
 */
public class IterativeParser {

	private CommandUtil<Operator> commandUtil;
	private TerminalParser terminalParser;
	private Operator operators[];

	public IterativeParser(Operator operators[]) {
		this(Singleton.get().getGrandContext(), operators);
	}

	private IterativeParser(Context context, Operator operators[]) {
		this.operators = operators;
		Map<String, Operator> operatorsByName = new HashMap<>();

		for (Operator operator : operators)
			if (operator != TermOp.TUPLE_)
				operatorsByName.put(operator.getName(), operator);

		commandUtil = new CommandUtil<>(operatorsByName);
		terminalParser = new TerminalParser(context);
	}

	public Node parse(String in0) {
		String in1 = Transform.transform(TransformerFactory.create(operators), in0).t0;
		return new Parse(in1).parse();
	}

	private enum LexType {
		CHAR__, HEX__, ID___, OPER_, SPACE, STR__, SYM__
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
		private Token token0;

		private Lex(String in) {
			this.in = in;
		}

		private Token lex() {
			return token0 = lex0();
		}

		private Token lex0() {
			if (pos < in.length()) {
				int start = pos;
				Token token = detect();
				LexType type = token.type;

				if (type == LexType.ID___ || type == LexType.SPACE)
					while (pos < in.length() && detect().type == type)
						pos++;
				else if (type == LexType.CHAR__)
					pos += 4;
				else if (type == LexType.HEX__) {
					pos += 2;
					while (pos < in.length() && "0123456789ABCDEF".indexOf(in.charAt(pos)) >= 0)
						pos++;
				} else if (type == LexType.OPER_)
					pos += token.operator.getName().length();
				else if (type == LexType.STR__) {
					char quote = in.charAt(pos);
					while (pos < in.length() && in.charAt(pos) == quote) {
						pos++;
						while (pos < in.length() && in.charAt(pos) != quote)
							pos++;
						pos++;
					}
				} else if (type == LexType.SYM__)
					pos++;

				token.data = in.substring(start, pos);

				if (type == LexType.SPACE) {
					List<Integer> precs = new ArrayList<>();

					for (Token t : Arrays.asList(token0, detect()))
						if (t != null && t.operator != null)
							precs.add(t.operator.getPrecedence());

					if (!precs.isEmpty() && Collections.min(precs) > TermOp.TUPLE_.getPrecedence()) {
						token.type = LexType.OPER_;
						token.operator = TermOp.TUPLE_;
					} else
						token = lex0();
				}

				return token;
			} else
				return null;
		}

		private Token detect() {
			LexType type;
			Operator operator = Pair.first_(commandUtil.recognize(in, pos));

			if (pos < in.length()) {
				char ch = in.charAt(pos);

				if (operator != null)
					type = LexType.OPER_;
				else if (ch == '+' && pos + 4 < in.length() && in.charAt(pos + 1) == '\'')
					type = LexType.CHAR__;
				else if (ch == '+' && pos + 1 < in.length() && in.charAt(pos + 1) == 'x')
					type = LexType.HEX__;
				else if (ch == ' ')
					type = LexType.SPACE;
				else if (ch == '\'' || ch == '"')
					type = LexType.STR__;
				else if (ch == '(' || ch == '[' || ch == '{' //
						|| ch == ')' || ch == ']' || ch == '}' //
						|| ch == '`')
					type = LexType.SYM__;
				else
					type = LexType.ID___;
			} else
				type = null;

			return new Token(type, operator);
		}
	}

	private class Section {
		private char kind;
		private List<Tree> list = new ArrayList<>(Arrays.asList(Tree.of(null, null, Atom.NIL)));
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
							node = Tree.of(TermOp.TUPLE_, Atom.of("["), node);
						add(node);
					} else
						throw new RuntimeException("Cannot parse " + in);
				} else if (ch == '`')
					if (stack.peek().kind == ch) {
						Node node = stack.pop().first().getRight();
						node = Tree.of(TermOp.TUPLE_, Atom.of("`"), node);
						add(node);
					} else
						stack.push(new Section(ch));
				else if (Util.isNotBlank(data))
					add(terminalParser.parseTerminal(data));
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

			Tree tree1 = Tree.of(operator, tree.getRight(), Atom.NIL);
			Tree.forceSetRight(tree, tree1);
			list.subList(listPos + 1, list.size()).clear();
			section.push(tree1);
		}
	}

}
