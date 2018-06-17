package suite.node.parser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.parser.Lexer.Token;
import suite.node.tree.TreeTuple;
import suite.node.util.Context;
import suite.node.util.Singleton;
import suite.text.Preprocess;
import suite.util.Fail;
import suite.util.String_;

/**
 * Non-recursive, performance-improved parser for operator-based languages.
 *
 * @author ywsing
 */
public class IterativeParser {

	private TerminalParser terminalParser;
	private Operator[] operators;

	public IterativeParser(Operator[] operators) {
		this(Singleton.me.grandContext, operators);
	}

	private IterativeParser(Context context, Operator[] operators) {
		this.operators = operators;
		terminalParser = new TerminalParser(context);
	}

	public Node parse(String in0) {
		var in1 = Preprocess.transform(PreprocessorFactory.create(operators), in0).t0;
		return new Parse(in1).parse();
	}

	private class Parse {
		private String in;
		private Deque<Section> stack = new ArrayDeque<>();

		private Parse(String in) {
			this.in = in;
		}

		private Node parse() {
			var lex = new Lexer(operators, in);
			stack.push(new Section(' '));
			Token token;

			while ((token = lex.lex()) != null) {
				var operator = token.operator;
				var data = token.getData();
				var ch = data.charAt(0);

				if (operator != null) {
					addOperator(operator);
					if (operator == TermOp.BRACES)
						stack.push(new Section('{'));
				} else if (ch == '(' || ch == '[' || ch == '{')
					stack.push(new Section(ch));
				else if (ch == ')' || ch == ']' || ch == '}') {
					var section = stack.pop();

					if (section.kind == '(' && ch == ')' //
							|| section.kind == '[' && ch == ']' //
							|| section.kind == '{' && ch == '}') {
						var node = section.unwind(null).getRight();
						if (ch == ']')
							node = TreeTuple.of(Atom.of("["), node);
						add(node);
					} else
						Fail.t("cannot parse " + in);
				} else if (ch == '`')
					if (stack.peek().kind == ch) {
						var node = stack.pop().unwind(null).getRight();
						node = TreeTuple.of(Atom.of("`"), node);
						add(node);
					} else
						stack.push(new Section(ch));
				else if (String_.isNotBlank(data))
					add(terminalParser.parseTerminal(data));
			}

			if (stack.size() == 1)
				return stack.pop().unwind(null).getRight();
			else
				return Fail.t("cannot parse " + in);
		}

		private void add(Node node) {
			var section = stack.peek();
			if (!section.isDanglingRight)
				addOperator(TermOp.TUPLE_);
			Tree.forceSetRight(section.list.getLast(), node);
			section.isDanglingRight = false;
		}

		private void addOperator(Operator operator) {
			var section = stack.peek();
			var tree = section.unwind(operator);
			var tree1 = Tree.of(operator, tree.getRight(), Atom.NIL);
			Tree.forceSetRight(tree, tree1);
			section.push(tree1);
		}
	}

	private class Section {
		private char kind;
		private Deque<Tree> list = new ArrayDeque<>(List.of(Tree.of(null, null, Atom.NIL)));
		private boolean isDanglingRight = true;

		public Section(char kind) {
			this.kind = kind;
		}

		private Tree unwind(Operator operator) {
			var prec0 = operator != null ? operator.precedence() : -1;
			Operator op;
			Tree tree;

			while ((op = (tree = list.getLast()).getOperator()) != null) {
				var prec1 = op.precedence();
				if (prec0 < prec1 || operator.assoc() == Assoc.LEFT && prec0 == prec1)
					pop();
				else
					break;
			}
			return tree;
		}

		private void push(Tree tree) {
			list.addLast(tree);
			isDanglingRight = true;
		}

		private Tree pop() {
			return list.removeLast();
		}
	}

}
