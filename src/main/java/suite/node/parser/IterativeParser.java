package suite.node.parser;

import static primal.statics.Fail.fail;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import primal.Verbs.Is;
import primal.fp.Funs.Sink;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.parser.Lexer.Token;
import suite.node.tree.TreeTuple;
import suite.text.Preprocess;

/**
 * Non-recursive, near O(length) parser for operator-based languages.
 *
 * @author ywsing
 */
public class IterativeParser {

	private TerminalParser terminalParser = new TerminalParser();
	private Operator[] operators;

	public IterativeParser(Operator[] operators) {
		this.operators = operators;
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
					list.removeLast();
				else
					break;
			}
			return tree;
		}

		private void push(Tree tree) {
			list.addLast(tree);
			isDanglingRight = true;
		}
	}

	public Node parse(String in0) {
		var in = Preprocess.transform(PreprocessorFactory.create(operators), in0).k;
		var stack = new ArrayDeque<Section>();

		Sink<Operator> addOperator = operator -> {
			var section = stack.peek();
			var tree = section.unwind(operator);
			var tree0 = tree.getRight();
			var tree1 = Tree.of(operator, tree0, Atom.NIL);
			Tree.forceSetRight(tree, tree1);
			section.push(tree1);
		};

		Sink<Node> add = node -> {
			var section = stack.peek();
			if (!section.isDanglingRight)
				addOperator.f(TermOp.TUPLE_);
			Tree.forceSetRight(section.list.getLast(), node);
			section.isDanglingRight = false;
		};

		var lex = new Lexer(operators, in);
		stack.push(new Section(' '));
		Token token;

		while ((token = lex.lex()) != null) {
			var operator = token.operator;
			var data = token.getData();
			var ch = data.charAt(0);

			if (operator != null) {
				addOperator.f(operator);
				if (operator == TermOp.BRACES)
					stack.push(new Section('{'));
			} else if (ch == '(' || ch == '[' || ch == '{')
				stack.push(new Section(ch));
			else if (ch == ')' || ch == ']' || ch == '}') {
				var section = stack.pop();
				var kind = section.kind;

				if (kind == '(' && ch == ')' || kind == '[' && ch == ']' || kind == '{' && ch == '}') {
					var node = section.unwind(null).getRight();
					if (ch == ']')
						node = TreeTuple.of(Atom.of("["), node);
					else if (ch == '}')
						node = TreeTuple.of(Atom.of("{"), node);
					add.f(node);
				} else
					fail("cannot parse " + in);
			} else if (ch == '`')
				if (stack.peek().kind == ch) {
					var node = stack.pop().unwind(null).getRight();
					node = TreeTuple.of(Atom.of("`"), node);
					add.f(node);
				} else
					stack.push(new Section(ch));
			else if (Is.notBlank(data))
				add.f(terminalParser.parseTerminal(data));
		}

		return stack.size() == 1 ? stack.pop().unwind(null).getRight() : fail("cannot parse " + in);
	}

}
