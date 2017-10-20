package suite.funp;

import suite.Suite;
import suite.adt.pair.Pair;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpFixed;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPolyType;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpVariable;
import suite.immutable.ISet;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.util.Switch;

public class P0Parse {

	public Funp parse(Node node) {
		return new Parse(new ISet<>()).parse(node);
	}

	private class Parse {
		private ISet<String> variables;

		private Parse(ISet<String> variables) {
			this.variables = variables;
		}

		private Funp parse(Node node) {
			Node[] m;

			if ((m = Suite.match(".0 | .1").apply(node)) != null)
				return FunpApply.of(parse(m[0]), parse(m[1]));
			else if ((m = Suite.match("array .0").apply(node)) != null)
				return FunpArray.of(Tree.iter(m[0], TermOp.AND___).map(this::parse).toList());
			else if (node == Atom.FALSE)
				return FunpBoolean.of(false);
			else if (node == Atom.TRUE)
				return FunpBoolean.of(true);
			else if ((m = Suite.match("define .0 := .1 >> .2").apply(node)) != null) {
				String var = name(m[0]);
				return FunpDefine.of(var, FunpPolyType.of(parse(m[1])), parseNewVariable(m[2], var));
				// return parse(Suite.substitute("poly .1 | (.0 => .2)", m));
			} else if ((m = Suite.match("let .0 := .1 >> .2").apply(node)) != null) {
				String var = name(m[0]);
				return FunpDefine.of(var, parse(m[1]), parseNewVariable(m[2], var));
			}
			// return parse(Suite.substitute(".1 | (.0 => .2)", m));
			else if ((m = Suite.match("^.0").apply(node)) != null)
				return FunpDeref.of(parse(m[0]));
			else if ((m = Suite.match(".0/.1").apply(node)) != null)
				return FunpField.of(FunpReference.of(parse(m[0])), name(m[1]));
			else if ((m = Suite.match("fixed .0 => .1").apply(node)) != null)
				return FunpFixed.of(name(m[0]), parse(m[1]));
			else if ((m = Suite.match("if (`.0` := .1) then .2 else .3").apply(node)) != null)
				return bind(parse(m[0]), parse(m[1]), parse(m[2]), parse(m[3]));
			else if ((m = Suite.match("if .0 then .1 else .2").apply(node)) != null)
				return FunpIf.of(parse(m[0]), parse(m[1]), parse(m[2]));
			else if ((m = Suite.match(".0 {.1}").apply(node)) != null)
				return FunpIndex.of1(FunpReference.of(parse(m[0])), parse(m[1]));
			else if ((m = Suite.match(".0 => .1").apply(node)) != null)
				return FunpLambda.of(name(m[0]), parse(m[1]));
			else if (node instanceof Int)
				return FunpNumber.of(((Int) node).number);
			else if ((m = Suite.match("poly .0").apply(node)) != null)
				return FunpPolyType.of(parse(m[0]));
			else if ((m = Suite.match("address .0").apply(node)) != null)
				return FunpReference.of(parse(m[0]));
			else if ((m = Suite.match("struct .0").apply(node)) != null)
				return FunpStruct.of(Tree //
						.iter(m[0], TermOp.AND___) //
						.map(n -> {
							Node[] m1 = Suite.match(".0 .1").apply(n);
							return Pair.of(name(m1[0]), parse(m1[1]));
						}) //
						.toList());
			else if (node instanceof Tree) {
				Tree tree = (Tree) node;
				Funp left = parse(tree.getLeft());
				Funp right = parse(tree.getRight());
				return FunpTree.of(tree.getOperator(), left, right);
			} else if (node instanceof Atom)
				return FunpVariable.of(name(node));
			else
				throw new RuntimeException("cannot parse " + node);
		}

		private Funp bind(Funp be, Funp value, Funp then, Funp else_) {
			Switch<Funp> sw = new Switch<Funp>(be);

			sw.applyIf(FunpArray.class, f -> f.apply(elements -> {
				int size = elements.size();
				Funp then_ = then;
				for (int i = 0; i < size; i++)
					then_ = bind(elements.get(i), FunpIndex.of1(FunpReference.of(value), FunpNumber.of(i)), then_, else_);
				return then_;
			}));

			sw.applyIf(FunpStruct.class, f -> f.apply(pairs -> {
				Funp then_ = then;
				for (Pair<String, Funp> pair : pairs)
					then_ = bind(pair.t1, FunpField.of(FunpReference.of(value), pair.t0), then_, else_);
				return then_;
			}));

			sw.applyIf(FunpVariable.class, f -> f.apply(var -> {
				return variables.contains(var) ? be : FunpDefine.of(var, value, then);
			}));

			Funp result = sw.result();

			return result != null ? result : FunpIf.of(FunpTree.of(TermOp.EQUAL_, be, value), then, else_);
		}

		private Funp parseNewVariable(Node node, String var) {
			return new Parse(variables.add(var)).parse(node);
		}
	}

	private String name(Node node) {
		return ((Atom) node).name;
	}

}
