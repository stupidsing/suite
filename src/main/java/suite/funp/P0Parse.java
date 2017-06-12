package suite.funp;

import suite.Suite;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpFixed;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPolyType;
import suite.funp.P0.FunpVariable;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;

public class P0Parse {

	public Funp parse(Node node) {
		Node[] m;

		if ((m = Suite.match(".0 | .1").apply(node)) != null)
			return new FunpApply(parse(m[0]), parse(m[1]));
		else if (node == Atom.FALSE)
			return new FunpBoolean(false);
		else if (node == Atom.TRUE)
			return new FunpBoolean(true);
		else if ((m = Suite.match("define .0 := .1 >> .2").apply(node)) != null)
			return parse(Suite.substitute("poly .1 | (.0 => .2)", m));
		else if ((m = Suite.match("fixed .0 => .1").apply(node)) != null)
			return new FunpFixed(name(m[0]), parse(m[1]));
		else if ((m = Suite.match("if .0 then .1 else .2").apply(node)) != null)
			return new FunpIf(parse(m[0]), parse(m[1]), parse(m[2]));
		else if ((m = Suite.match("let .0 := .1 >> .2").apply(node)) != null)
			return parse(Suite.substitute(".1 | (.0 => .2)", m));
		else if ((m = Suite.match(".0 => .1").apply(node)) != null)
			return new FunpLambda(name(m[0]), parse(m[1]));
		else if (node instanceof Int)
			return new FunpNumber(((Int) node).number);
		else if ((m = Suite.match("poly .0").apply(node)) != null)
			return new FunpPolyType(parse(m[0]));
		else if (node instanceof Atom)
			return new FunpVariable(name(node));
		else if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Funp n0 = new FunpVariable(tree.getOperator().getName());
			Funp n1 = new FunpApply(n0, parse(tree.getLeft()));
			Funp n2 = new FunpApply(n1, parse(tree.getRight()));
			return n2;
		} else
			throw new RuntimeException("cannot parse " + node);
	}

	private String name(Node node) {
		return ((Atom) node).name;
	}

}
