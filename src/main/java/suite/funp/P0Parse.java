package suite.funp;

import suite.Suite;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpFixed;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPolyType;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpVariable;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;

public class P0Parse {

	public Funp parse(Node node) {
		Node[] m;

		if ((m = Suite.match(".0 | .1").apply(node)) != null)
			return FunpApply.of(parse(m[0]), parse(m[1]));
		else if ((m = Suite.match(".0 {.1}").apply(node)) != null)
			return FunpIndex.of(parse(m[0]), parse(m[1]));
		else if ((m = Suite.match("^.0").apply(node)) != null)
			return FunpDeref.of(parse(m[0]));
		else if (node == Atom.FALSE)
			return FunpBoolean.of(false);
		else if (node == Atom.TRUE)
			return FunpBoolean.of(true);
		else if ((m = Suite.match("array .0").apply(node)) != null)
			return FunpArray.of(Tree.iter(m[0], TermOp.AND___).map(this::parse).toList());
		else if ((m = Suite.match("define .0 := .1 >> .2").apply(node)) != null)
			return FunpDefine.of(name(m[0]), FunpPolyType.of(parse(m[1])), parse(m[2]));
		// return parse(Suite.substitute("poly .1 | (.0 => .2)", m));
		else if ((m = Suite.match("fixed .0 => .1").apply(node)) != null)
			return FunpFixed.of(name(m[0]), parse(m[1]));
		else if ((m = Suite.match("if .0 then .1 else .2").apply(node)) != null)
			return FunpIf.of(parse(m[0]), parse(m[1]), parse(m[2]));
		else if ((m = Suite.match("let .0 := .1 >> .2").apply(node)) != null)
			return FunpDefine.of(name(m[0]), parse(m[1]), parse(m[2]));
		// return parse(Suite.substitute(".1 | (.0 => .2)", m));
		else if ((m = Suite.match(".0 => .1").apply(node)) != null)
			return FunpLambda.of(name(m[0]), parse(m[1]));
		else if (node instanceof Int)
			return FunpNumber.of(((Int) node).number);
		else if ((m = Suite.match("poly .0").apply(node)) != null)
			return FunpPolyType.of(parse(m[0]));
		else if ((m = Suite.match("address .0").apply(node)) != null)
			return FunpReference.of(parse(m[0]));
		else if (node instanceof Atom)
			return FunpVariable.of(name(node));
		else if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Funp left = parse(tree.getLeft());
			Funp right = parse(tree.getRight());
			return FunpTree.of(tree.getOperator(), left, right);
		} else
			throw new RuntimeException("cannot parse " + node);
	}

	private String name(Node node) {
		return ((Atom) node).name;
	}

}
