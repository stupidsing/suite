package suite.funp;

import java.util.List;

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
import suite.funp.P0.FunpVerifyType;
import suite.immutable.ISet;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.primitive.IntPrimitives.Int_Obj;
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
			else if ((m = Suite.match("if (`.0` := .1) then .2 else .3").apply(node)) != null) {
				Funp v0 = parse(m[0]);
				Funp v1 = parse(m[1]);
				return FunpVerifyType.of(v0, v1, bind(v0, v1, parse(m[2]), parse(m[3])));
			} else if ((m = Suite.match("if .0 then .1 else .2").apply(node)) != null)
				return FunpIf.of(parse(m[0]), parse(m[1]), parse(m[2]));
			else if ((m = Suite.match(".0 {.1}").apply(node)) != null)
				return FunpIndex.of(FunpReference.of(parse(m[0])), parse(m[1]));
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
			String var;

			if (be instanceof FunpBoolean && value instanceof FunpBoolean)
				return ((FunpBoolean) be).b == ((FunpBoolean) value).b ? then : else_;
			else if (be instanceof FunpNumber && value instanceof FunpNumber)
				return ((FunpNumber) be).i == ((FunpNumber) value).i ? then : else_;
			else if (be instanceof FunpVariable && !variables.contains(var = ((FunpVariable) be).var))
				return FunpDefine.of(var, value, then);
			else {
				Switch<Funp> sw0 = new Switch<Funp>(be);

				sw0.applyIf(FunpArray.class, f -> f.apply(elements0 -> {
					List<Funp> elements1 = new Switch<List<Funp>>(value).applyIf(FunpArray.class, g -> g.elements).result();
					int size0 = elements0.size();
					Funp then_ = then;
					Int_Obj<Funp> fun;

					if (Boolean.FALSE && elements1 != null && size0 == elements1.size())
						fun = elements1::get;
					else
						fun = i -> FunpIndex.of(FunpReference.of(value), FunpNumber.of(i));

					for (int i = 0; i < size0; i++)
						then_ = bind(elements0.get(i), fun.apply(i), then_, else_);

					return then_;
				})).applyIf(FunpStruct.class, f -> f.apply(pairs0 -> {
					List<Pair<String, Funp>> pairs1 = new Switch<List<Pair<String, Funp>>>(value)
							.applyIf(FunpStruct.class, g -> g.pairs) //
							.result();

					int size0 = pairs0.size();
					Funp then_ = then;
					Int_Obj<Funp> fun;

					if (Boolean.FALSE && pairs1 != null && size0 == pairs1.size())
						fun = i -> pairs1.get(i).t1;
					else
						fun = i -> FunpField.of(FunpReference.of(value), pairs0.get(i).t0);

					for (int i = 0; i < size0; i++)
						then_ = bind(pairs0.get(i).t1, fun.apply(i), then_, else_);

					return then_;
				}));

				Funp result = sw0.result();

				return result != null ? result : FunpIf.of(FunpTree.of(TermOp.EQUAL_, be, value), then, else_);
			}
		}

		private Funp parseNewVariable(Node node, String var) {
			return new Parse(variables.add(var)).parse(node);
		}
	}

	private String name(Node node) {
		return ((Atom) node).name;
	}

}
