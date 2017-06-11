package suite.funp;

import suite.Suite;
import suite.funp.Funp_.Funp;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;

public class FunpParse {

	private Funp_ funp_ = new Funp_();

	public Funp parse(Node node) {
		Node[] m;

		if ((m = Suite.match(".0 | .1").apply(node)) != null)
			return funp_.new FunpApply(parse(m[0]), parse(m[1]));
		else if (node == Atom.FALSE)
			return funp_.new FunpBoolean(false);
		else if (node == Atom.TRUE)
			return funp_.new FunpBoolean(true);
		else if ((m = Suite.match("define .0 := .1 >> .2").apply(node)) != null)
			return parse(Suite.substitute("poly .1 | (.0 => .2)", m));
		else if ((m = Suite.match("fixed .0 => .1").apply(node)) != null)
			return funp_.new FunpFixed(name(m[0]), parse(m[1]));
		else if ((m = Suite.match("if .0 then .1 else .2").apply(node)) != null)
			return funp_.new FunpIf(parse(m[0]), parse(m[1]), parse(m[2]));
		else if ((m = Suite.match("let .0 := .1 >> .2").apply(node)) != null)
			return parse(Suite.substitute(".1 | (.0 => .2)", m));
		else if ((m = Suite.match(".0 => .1").apply(node)) != null)
			return funp_.new FunpLambda(name(m[0]), parse(m[1]));
		else if (node instanceof Int)
			return funp_.new FunpNumber(((Int) node).number);
		else if ((m = Suite.match("poly .0").apply(node)) != null)
			return funp_.new FunpPolyType(parse(m[0]));
		else if (node instanceof Atom)
			return funp_.new FunpVariable(name(node));
		else if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Funp f0 = funp_.new FunpVariable(tree.getOperator().getName());
			Funp f1 = funp_.new FunpApply(f0, parse(tree.getLeft()));
			Funp f2 = funp_.new FunpApply(f1, parse(tree.getRight()));
			return f2;
		} else
			throw new RuntimeException();
	}

	private String name(Node node) {
		return ((Atom) node).name;
	}

}
