package suite.funp;

import suite.Suite;
import suite.funp.Funp_.Funp;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;

public class P0Parse {

	private P0 funpk = new P0();

	public Funp parse(Node node) {
		Node[] m;

		if ((m = Suite.match(".0 | .1").apply(node)) != null)
			return funpk.new FunpApply(parse(m[0]), parse(m[1]));
		else if (node == Atom.FALSE)
			return funpk.new FunpBoolean(false);
		else if (node == Atom.TRUE)
			return funpk.new FunpBoolean(true);
		else if ((m = Suite.match("define .0 := .1 >> .2").apply(node)) != null)
			return parse(Suite.substitute("poly .1 | (.0 => .2)", m));
		else if ((m = Suite.match("fixed .0 => .1").apply(node)) != null)
			return funpk.new FunpFixed(name(m[0]), parse(m[1]));
		else if ((m = Suite.match("if .0 then .1 else .2").apply(node)) != null)
			return funpk.new FunpIf(parse(m[0]), parse(m[1]), parse(m[2]));
		else if ((m = Suite.match("let .0 := .1 >> .2").apply(node)) != null)
			return parse(Suite.substitute(".1 | (.0 => .2)", m));
		else if ((m = Suite.match(".0 => .1").apply(node)) != null)
			return funpk.new FunpLambda(name(m[0]), parse(m[1]));
		else if (node instanceof Int)
			return funpk.new FunpNumber(((Int) node).number);
		else if ((m = Suite.match("poly .0").apply(node)) != null)
			return funpk.new FunpPolyType(parse(m[0]));
		else if (node instanceof Atom)
			return funpk.new FunpVariable(name(node));
		else if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Funp f0 = funpk.new FunpVariable(tree.getOperator().getName());
			Funp f1 = funpk.new FunpApply(f0, parse(tree.getLeft()));
			Funp f2 = funpk.new FunpApply(f1, parse(tree.getRight()));
			return f2;
		} else
			throw new RuntimeException();
	}

	private String name(Node node) {
		return ((Atom) node).name;
	}

}
