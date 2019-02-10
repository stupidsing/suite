package suite.node.parser;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.parser.FactorizeResult.FNode;
import suite.node.parser.FactorizeResult.FTerminal;
import suite.node.parser.FactorizeResult.FTree;
import suite.node.tree.TreeTuple;

/**
 * Recursive-descent parser for operator-based languages.
 *
 * @author ywsing
 */
public class RecursiveParser {

	private Operator[] operators;
	private TerminalParser terminalParser = new TerminalParser();

	public RecursiveParser(Operator[] operators) {
		this.operators = operators;
	}

	public Node parse(String in) {
		return node(new RecursiveFactorizer(operators).parse(in).node);
	}

	private Node node(FNode fn) {
		if (fn instanceof FTree) {
			var ft = (FTree) fn;
			var name = ft.name;
			var pairs = ft.pairs;
			var fn0 = pairs.get(0).node;
			var fn1 = pairs.get(1).node;
			var fn2 = pairs.get(2).node;

			if (name.equals("("))
				return node(fn1);
			else if (name.equals("["))
				return TreeTuple.of(Atom.of("[]"), node(fn1));
			else if (name.equals("`"))
				return TreeTuple.of(Atom.of("`"), node(fn1));
			else
				return Tree.of(TermOp.valueOf(name), node(fn0), node(fn2));
		} else
			return terminalParser.parseTerminal(((FTerminal) fn).chars.toString());
	}

}
