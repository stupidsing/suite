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
			return FunpApply.of(parse(m[0]), parse(m[1]));
		else if (node == Atom.FALSE)
			return FunpBoolean.of(false);
		else if (node == Atom.TRUE)
			return FunpBoolean.of(true);
		else if ((m = Suite.match("define .0 := .1 >> .2").apply(node)) != null)
			return parse(Suite.substitute("poly .1 | (.0 => .2)", m));
		else if ((m = Suite.match("fixed .0 => .1").apply(node)) != null)
			return FunpFixed.of(name(m[0]), parse(m[1]));
		else if ((m = Suite.match("if .0 then .1 else .2").apply(node)) != null)
			return FunpIf.of(parse(m[0]), parse(m[1]), parse(m[2]));
		else if ((m = Suite.match("let .0 := .1 >> .2").apply(node)) != null)
			return parse(Suite.substitute(".1 | (.0 => .2)", m));
		else if ((m = Suite.match(".0 => .1").apply(node)) != null)
			return FunpLambda.of(name(m[0]), parse(m[1]));
		else if (node instanceof Int)
			return FunpNumber.of(((Int) node).number);
		else if ((m = Suite.match("poly .0").apply(node)) != null)
			return FunpPolyType.of(parse(m[0]));
		else if (node instanceof Atom)
			return FunpVariable.of(name(node));
		else if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Funp n0 = FunpVariable.of(tree.getOperator().getName());
			Funp n1 = FunpApply.of(n0, parse(tree.getLeft()));
			Funp n2 = FunpApply.of(n1, parse(tree.getRight()));
			return n2;
		} else
			throw new RuntimeException("cannot parse " + node);
	}

	private String name(Node node) {
		return ((Atom) node).name;
	}

}
