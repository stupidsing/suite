package suite.node.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.parser.Lexer.Token;
import suite.node.util.Context;
import suite.node.util.Singleton;
import suite.text.Preprocess;
import suite.util.Util;

/**
 * Non-recursive, performance-improved parser for operator-based languages.
 *
 * @author ywsing
 */
public class IterativeParser {

	private TerminalParser terminalParser;
	private Operator operators[];

	public IterativeParser(Operator operators[]) {
		this(Singleton.get().getGrandContext(), operators);
	}

	private IterativeParser(Context context, Operator operators[]) {
		this.operators = operators;
		terminalParser = new TerminalParser(context);
	}

	public Node parse(String in0) {
		String in1 = Preprocess.transform(PreprocessorFactory.create(operators), in0).t0;
		return new Parse(in1).parse();
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
			Lexer lex = new Lexer(operators, in);
			stack.push(new Section(' '));
			Token token;

			while ((token = lex.lex()) != null) {
				Operator operator = token.operator;
				String data = token.getData();
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
