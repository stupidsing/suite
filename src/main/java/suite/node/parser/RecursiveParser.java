package suite.node.parser;

import java.util.List;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.parser.RecursiveFactorizer.FNode;
import suite.node.parser.RecursiveFactorizer.FTerminal;
import suite.node.parser.RecursiveFactorizer.FTree;
import suite.node.util.Singleton;
import suite.streamlet.Read;

/**
 * Recursive-descent parser for operator-based languages.
 *
 * @author ywsing
 */
public class RecursiveParser {

	private Operator operators[];
	private TerminalParser terminalParser = new TerminalParser(Singleton.get().getGrandContext());

	public RecursiveParser(Operator operators[]) {
		this.operators = operators;
	}

	public Node parse(String in) {
		return node(new RecursiveFactorizer(operators).parse(in));
	}

	private Node node(FNode fn) {
		if (fn instanceof FTree) {
			FTree ft = (FTree) fn;
			List<FNode> fns = ft.fns;
			FNode fn0 = fns.get(0);
			FNode fn1 = fns.get(1);
			FNode fn2 = fns.get(2);

			if (ft.name.equals("("))
				return node(fn1);
			else if (ft.name.equals("["))
				return Tree.of(TermOp.TUPLE_, Atom.of("[]"), node(fn1));
			if (ft.name.equals("`"))
				return Tree.of(TermOp.TUPLE_, Atom.of("`"), node(fn1));
			else {
				Operator operator = Read.from(operators).filter(op -> op.getName() == ft.name).uniqueResult();
				return Tree.of(operator, node(fn0), node(fn2));
			}
		} else
			return terminalParser.parseTerminal(str(fn));
	}

	private String str(FNode fn) {
		return fn instanceof FTerminal ? ((FTerminal) fn).chars.toString() : null;
	}

}
