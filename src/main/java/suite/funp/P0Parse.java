package suite.funp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import suite.BindArrayUtil.Match;
import suite.Suite;
import suite.adt.pair.Pair;
import suite.assembler.Amd64;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpAsm;
import suite.funp.P0.FunpAssignReference;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpCheckType;
import suite.funp.P0.FunpCoerce;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpError;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpFixed;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpIterate;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPolyType;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpRepeat;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpVariable;
import suite.funp.P0.FunpVariableNew;
import suite.immutable.IMap;
import suite.immutable.ISet;
import suite.inspect.Inspect;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.lp.kb.Prototype;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.Singleton;
import suite.primitive.IntPrimitives.IntObj_Obj;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.streamlet.As;
import suite.streamlet.Streamlet;
import suite.util.Switch;

public class P0Parse {

	private Atom dontCare = Atom.of("_");
	private Inspect inspect = Singleton.me.inspect;

	public Funp parse(Node node0) {
		class Expand {
			private IMap<Prototype, Pair<Node, Node>> macros;

			private Expand(IMap<Prototype, Pair<Node, Node>> macros) {
				this.macros = macros;
			}

			private Node expand(Node node) {
				Tree tree;
				Node[] m;
				Pair<Node, Node> expand;

				if ((m = Suite.match("expand .0 := .1 >> .2").apply(node)) != null) {
					Node head = m[0];
					return new Expand(macros.put(Prototype.of(head), Pair.of(head, m[1]))).expand(m[2]);
				} else if ((expand = macros.get(Prototype.of(node))) != null) {
					Generalizer g = new Generalizer();
					Node t0_ = g.generalize(expand.t0);
					Node t1_ = g.generalize(expand.t1);
					System.out.println(node);
					System.out.println(t0_);
					if (Binder.bind(node, t0_, new Trail()))
						return expand(t1_);
				}

				if ((tree = Tree.decompose(node)) != null)
					return Tree.of(tree.getOperator(), expand(tree.getLeft()), expand(tree.getRight()));
				else
					return node;
			}
		}

		Node node1 = new Expand(IMap.empty()).expand(node0);
		return new Parse(new ISet<>()).parse(node1);
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
			else if ((m = Suite.match("asm .0 {.1}").apply(node)) != null)
				return FunpAsm.of(Tree.iter(m[0], TermOp.OR____).map(n -> {
					Node[] ma = Suite.match(".0 = .1").apply(n);
					return Pair.of(Amd64.me.regsByName.get(ma[0]), parse(ma[1]));
				}).toList(), Tree.iter(m[1], TermOp.OR____).toList());
			else if (node == Atom.FALSE)
				return FunpBoolean.of(false);
			else if (node == Atom.TRUE)
				return FunpBoolean.of(true);
			else if ((m = Suite.match("type .0 = .1 >> .2").apply(node)) != null)
				return FunpCheckType.of(parse(m[0]), parse(m[1]), parse(m[2]));
			else if ((m = Suite.match("byte .0").apply(node)) != null)
				return FunpCoerce.of("byte", parse(m[0]));
			else if ((m = Suite.match("define .0 := .1 >> .2").apply(node)) != null) {
				String var = name(m[0]);
				return FunpDefine.of(var, FunpPolyType.of(parse(m[1])), parseNewVariable(m[2], var));
				// return parse(Suite.substitute("poly .1 | (.0 => .2)", m));
			} else if ((m = Suite.match("recurse .0 >> .1").apply(node)) != null) {
				Match match1 = Suite.match(".0 := .1");
				Streamlet<Node[]> list = Tree.iter(m[0], TermOp.AND___).map(match1::apply).collect(As::streamlet);
				ISet<String> variables_ = variables;

				for (Node[] array : list)
					variables_ = variables_.add(name(array[0]));

				Parse parse1 = new Parse(variables_);

				return FunpDefineRec.of(list //
						.map(m1 -> {
							Funp value = FunpPolyType.of(parse1.parse(m1[1]));
							return Pair.of(name(m1[0]), value);
						}) //
						.toList(), parse1.parse(m[1]));
			} else if ((m = Suite.match("let .0 := .1 >> .2").apply(node)) != null) {
				String var = name(m[0]);
				return FunpDefine.of(var, parse(m[1]), parseNewVariable(m[2], var));
			}
			// return parse(Suite.substitute(".1 | (.0 => .2)", m));
			else if ((m = Suite.match("^.0").apply(node)) != null)
				return FunpDeref.of(parse(m[0]));
			else if (node == dontCare)
				return FunpDontCare.of();
			else if ((m = Suite.match("error").apply(node)) != null)
				return FunpError.of();
			else if ((m = Suite.match(".0/.1").apply(node)) != null)
				return FunpField.of(FunpReference.of(parse(m[0])), name(m[1]));
			else if ((m = Suite.match("fixed .0 => .1").apply(node)) != null) {
				String var = name(m[0]);
				return FunpFixed.of(var, parseNewVariable(m[1], var));
			} else if ((m = Suite.match("if (`.0` = .1) then .2 else .3").apply(node)) != null) {
				ExtractNewVariables env = new ExtractNewVariables();
				Funp be = env.extract(parse(m[0]));
				Funp value = parse(m[1]);
				ISet<String> variables1 = new ISet<>();

				for (String var : env.variables)
					variables1 = variables1.add(var);

				Bind bind = new Bind(env.variables);
				Funp f = FunpCheckType.of(be, value, bind.bind(be, value, new Parse(variables1).parse(m[2]), parse(m[3])));
				for (String var : env.variables)
					f = FunpDefine.of(var, FunpDontCare.of(), f);
				return f;
			} else if ((m = Suite.match("if .0 then .1 else .2").apply(node)) != null)
				return FunpIf.of(parse(m[0]), parse(m[1]), parse(m[2]));
			else if ((m = Suite.match(".0 {.1}").apply(node)) != null)
				return FunpIndex.of(FunpReference.of(parse(m[0])), parse(m[1]));
			else if ((m = Suite.match("iterate .0 .1 .2 .3").apply(node)) != null) {
				String var = name(m[0]);
				Parse parse1 = new Parse(variables.add(var));
				return FunpIterate.of(var, parse(m[1]), parse1.parse(m[2]), parse1.parse(m[3]));
			} else if ((m = Suite.match("`.0` => .1").apply(node)) != null)
				return parse(Suite.match(".2 => if (`.0` = .2) then .1 else error").substitute(m[0], m[1], Atom.temp()));
			else if ((m = Suite.match(".0 => .1").apply(node)) != null) {
				String var = name(m[0]);
				return FunpLambda.of(var, parseNewVariable(m[1], var));
			} else if (node instanceof Int)
				return FunpNumber.ofNumber(((Int) node).number);
			else if ((m = Suite.match("poly .0").apply(node)) != null)
				return FunpPolyType.of(parse(m[0]));
			else if ((m = Suite.match("address .0").apply(node)) != null)
				return FunpReference.of(parse(m[0]));
			else if ((m = Suite.match(".0 * array .1").apply(node)) != null)
				return FunpRepeat.of(((Int) m[0]).number, parse(m[1]));
			else if ((m = Suite.match(".0, .1").apply(node)) != null)
				return FunpStruct.of(List.of(Pair.of("t0", parse(m[0])), Pair.of("t1", parse(m[1]))));
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
			} else if (node instanceof Atom) {
				String var = name(node);
				if (variables.contains(var))
					return FunpVariable.of(var);
				else
					return FunpVariableNew.of(var);
			} else
				throw new RuntimeException("cannot parse " + node);
		}

		private Funp parseNewVariable(Node node, String var) {
			return new Parse(variables.add(var)).parse(node);
		}
	}

	private class ExtractNewVariables {
		private Set<String> variables = new HashSet<>();

		private Funp extract(Funp be) {
			return inspect.rewrite(Funp.class, n_ -> {
				if (n_ instanceof FunpVariableNew) {
					String var = ((FunpVariableNew) n_).var;
					variables.add(var);
					return FunpVariable.of(var);
				} else
					return null;
			}, be);
		}
	}

	private class Bind {
		private Set<String> variables;

		private Bind(Set<String> variables) {
			this.variables = variables;
		}

		private Funp bind(Funp be, Funp value, Funp then, Funp else_) {
			IntObj_Obj<Int_Obj<Funp>, Funp> bindArray = (size0, fun0) -> {
				Int_Obj<Funp> fun1 = new Switch<Int_Obj<Funp>>(value //
				).applyIf(FunpArray.class, g -> {
					List<Funp> elements = g.elements;
					return size0 == elements.size() ? elements::get : null;
				}).applyIf(FunpRepeat.class, g -> g.apply((count, expr) -> {
					Int_Obj<Funp> fun_ = i -> expr;
					return size0 == count ? fun_ : null;
				})).applyIf(Funp.class, g -> {
					return i -> FunpIndex.of(FunpReference.of(value), FunpNumber.ofNumber(i));
				}).result();

				Funp then_ = then;
				for (int i = 0; i < size0; i++)
					then_ = bind(fun0.apply(i), fun1.apply(i), then_, else_);
				return then_;
			};

			if (be instanceof FunpBoolean && value instanceof FunpBoolean)
				return ((FunpBoolean) be).b == ((FunpBoolean) value).b ? then : else_;
			else if (be instanceof FunpNumber && value instanceof FunpNumber)
				return ((FunpNumber) be).i == ((FunpNumber) value).i ? then : else_;
			else {
				Funp result = new Switch<Funp>(be //
				).applyIf(FunpArray.class, f -> f.apply(elements0 -> {
					return bindArray.apply(elements0.size(), elements0::get);
				})).applyIf(FunpDontCare.class, f -> {
					return then;
				}).applyIf(FunpRepeat.class, f -> f.apply((size0, expr0) -> {
					return bindArray.apply(size0, i -> expr0);
				})).applyIf(FunpStruct.class, f -> f.apply(pairs0 -> {
					List<Pair<String, Funp>> pairs1 = new Switch<List<Pair<String, Funp>>>(value)
							.applyIf(FunpStruct.class, g -> g.pairs) //
							.result();

					int size0 = pairs0.size();
					Funp then_ = then;

					Int_Obj<Funp> fun = pairs1 != null && size0 == pairs1.size() //
							? i -> pairs1.get(i).t1 //
							: i -> FunpField.of(FunpReference.of(value), pairs0.get(i).t0);

					for (int i = 0; i < size0; i++)
						then_ = bind(pairs0.get(i).t1, fun.apply(i), then_, else_);

					return then_;
				})).applyIf(FunpVariable.class, f -> f.apply(var -> {
					return variables.contains(var) //
							? FunpAssignReference.of(FunpReference.of(FunpVariable.of(var)), value, then) //
							: be;
				})).result();

				return result != null ? result : FunpIf.of(FunpTree.of(TermOp.EQUAL_, be, value), then, else_);
			}
		}
	}

	private String name(Node node) {
		return ((Atom) node).name;
	}

}
