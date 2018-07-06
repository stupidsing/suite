package suite.funp;

import static suite.util.Friends.rethrow;

import java.io.IOException;
import java.util.List;

import suite.Suite;
import suite.adt.Mutable;
import suite.adt.pair.Pair;
import suite.assembler.Amd64;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpCheckType;
import suite.funp.P0.FunpCoerce;
import suite.funp.P0.FunpCoerce.Coerce;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefine.Fdt;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpError;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpIo;
import suite.funp.P0.FunpIoAsm;
import suite.funp.P0.FunpIoAssignRef;
import suite.funp.P0.FunpIoCat;
import suite.funp.P0.FunpIoFold;
import suite.funp.P0.FunpIoMap;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPredefine;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpRepeat;
import suite.funp.P0.FunpSizeOf;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpVariable;
import suite.funp.P0.FunpVariableNew;
import suite.http.HttpUtil;
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
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.SwitchNode;
import suite.node.io.TermOp;
import suite.node.util.Singleton;
import suite.os.FileUtil;
import suite.primitive.IntMutable;
import suite.primitive.IntPrimitives.IntObj_Obj;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.Ints_;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Iterate;
import suite.util.ReadStream;
import suite.util.Rethrow.SourceEx;
import suite.util.Switch;
import suite.util.To;
import suite.util.Util;

public class P0Parse {

	private Atom dontCare = Atom.of("_");
	private Inspect inspect = Singleton.me.inspect;

	public Funp parse(Node node0) {
		var node1 = new Expand(IMap.empty()).e(node0);
		return new Parse(ISet.empty()).p(node1);
	}

	private class Expand {
		private IMap<Prototype, Node[]> macros;

		private Expand(IMap<Prototype, Node[]> macros) {
			this.macros = macros;
		}

		private Node e(Node node) {
			Node[] m;

			if ((m = Suite.pattern("expand .0 := .1 ~ .2").match(node)) != null) {
				var head = m[0];
				return new Expand(macros.put(Prototype.of(head), new Node[] { head, m[1], })).e(m[2]);
			} else if ((m = macros.get(Prototype.of(node))) != null) {
				var g = new Generalizer();
				var t0_ = g.generalize(m[0]);
				var t1_ = g.generalize(m[1]);
				var trail = new Trail();

				if (Binder.bind(node, t0_, trail))
					return e(t1_);
				else
					trail.unwindAll();
			} else if ((m = Suite.pattern(".0 {.1}").match(node)) != null && m[0] != Atom.NIL)
				return Suite.substitute(".0 ({.1})", e(m[0]), e(m[1]));

			var tree = Tree.decompose(node);

			return tree != null ? Tree.of(tree.getOperator(), e(tree.getLeft()), e(tree.getRight())) : node;
		}
	}

	private class Parse {
		private ISet<String> variables;

		private Parse(ISet<String> variables) {
			this.variables = variables;
		}

		private Funp p(Node node) {
			return new SwitchNode<Funp>(node //
			).match(".0 | .1", (a, b) -> {
				return FunpApply.of(p(a), p(b));
			}).match(".0 .1", (a, b) -> {
				var name = a instanceof Atom ? Atom.name(a) : null;
				return name != null && variables.contains(name) ? FunpApply.of(p(b), p(a)) : null;
			}).match("[.0]", a -> {
				return FunpArray.of(Tree.iter(a).map(this::p).toList());
			}).match(Atom.FALSE, () -> {
				return FunpBoolean.of(false);
			}).match(Atom.TRUE, () -> {
				return FunpBoolean.of(true);
			}).match("type .0 = .1 ~ .2", (a, b, c) -> {
				return FunpCheckType.of(p(a), p(b), p(c));
			}).match("byte", () -> {
				return FunpCoerce.of(Coerce.BYTE, FunpDontCare.of());
			}).match("byte .0", a -> {
				return FunpCoerce.of(Coerce.BYTE, FunpNumber.ofNumber(Int.num(a)));
			}).match("coerce.byte .0", a -> {
				return FunpCoerce.of(Coerce.BYTE, p(a));
			}).match("coerce.number .0", a -> {
				return FunpCoerce.of(Coerce.NUMBER, p(a));
			}).match("coerce.pointer .0", a -> {
				return FunpCoerce.of(Coerce.POINTER, p(a));
			}).match("consult .0", a -> {
				return consult(Str.str(a));
			}).match("define .0 := .1 ~ .2", (a, b, c) -> {
				if (a instanceof Atom) {
					var var = Atom.name(a);
					return FunpDefine.of(Fdt.POLY, var, p(b), nv(var).p(c));
				} else
					return null;
				// return parse(Suite.subst("poly .1 | (.0 => .2)", m));
			}).match("let .0 := .1 ~ .2", (a, b, c) -> {
				var var = isVar(a) ? Atom.name(a) : null;
				if (var != null)
					return FunpDefine.of(Fdt.MONO, var, p(b), nv(var).p(c));
				// return parse(Suite.subst(".1 | (.0 => .2)", m));
				else
					return bind(a, b, c);
			}).match("let.global .0 := .1 ~ .2", (a, b, c) -> {
				var var = Atom.name(a);
				return FunpDefine.of(Fdt.GLOB, var, p(b), nv(var).p(c));
			}).match("define ({ .0 }) ~ .1", (a, b) -> {
				var list = Tree.iter(a, Tree::decompose).map(this::kv).collect();
				var variables1 = list.fold(variables, (vs, pair) -> vs.add(pair.t0));
				var p1 = new Parse(variables1);
				return FunpDefineRec.of(list //
						.map(pair -> Pair.of(pair.t0, p1.p(pair.t1))) //
						.toList(), p1.p(b));
			}).match("^.0", a -> {
				return FunpDeref.of(p(a));
			}).match(dontCare, () -> {
				return FunpDontCare.of();
			}).matchArray("error", m -> {
				return FunpError.of();
			}).match(".0/.1", (a, b) -> {
				return b instanceof Atom ? FunpField.of(FunpReference.of(p(a)), Atom.name(b)) : null;
			}).match("if (`.0` = .1) then .2 else .3", (a, b, c, d) -> {
				return bind(a, b, c, d);
			}).match("if .0 then .1 else .2", (a, b, c) -> {
				return FunpIf.of(p(a), p(b), p(c));
			}).match(".0/:.1", (a, b) -> {
				return FunpIndex.of(FunpReference.of(p(a)), p(b));
			}).match("io .0", a -> {
				return FunpIo.of(p(a));
			}).match("(io.asm .0) ({.1})", (a, b) -> {
				return FunpIoAsm.of(Tree.iter(a, TermOp.OR____).map(n -> {
					var ma = Suite.pattern(".0 = .1").match(n);
					return Pair.of(Amd64.me.regByName.get(ma[0]), p(ma[1]));
				}).toList(), Tree.iter(b, TermOp.OR____).toList());
			}).match("io.assign ^.0 := .1 ~ .2", (a, b, c) -> {
				return FunpIoAssignRef.of(FunpReference.of(p(a)), p(b), p(c));
			}).match("io.assign .0 := .1 ~ .2", (a, b, c) -> {
				return FunpIoAssignRef.of(FunpReference.of(FunpVariable.of(Atom.name(a))), p(b), p(c));
			}).match("io.let .0 := .1 ~ .2", (a, b, c) -> {
				String var;
				Funp f;
				if (isVar(a))
					f = nv(var = Atom.name(a)).p(c);
				else {
					var = "l$" + Util.temp();
					f = nv(var).bind(a, Atom.of(var), c);
				}
				return FunpApply.of(p(b), FunpIoCat.of(FunpLambda.of(var, f)));
			}).match("io.cat .0", a -> {
				return FunpIoCat.of(p(a));
			}).match("io.fold .0 .1 .2", (a, b, c) -> {
				return FunpIoFold.of(p(a), p(b), p(c));
			}).match("io.map .0", a -> {
				return FunpIoMap.of(p(a));
			}).match(".0 => .1", (a, b) -> {
				String var;
				Funp f;
				if (isVar(a))
					f = nv(var = Atom.name(a)).p(b);
				else {
					var = "l$" + Util.temp();
					f = nv(var).bind(a, Atom.of(var), b);
				}
				return FunpLambda.of(var, f);
			}).match("number", () -> {
				return FunpNumber.of(IntMutable.nil());
			}).applyIf(Int.class, n -> {
				return FunpNumber.ofNumber(n.number);
			}).match("predef .0", a -> {
				return FunpPredefine.of(p(a));
			}).match("address .0", a -> {
				return FunpReference.of(p(a));
			}).match("array .0 * .1", (a, b) -> {
				return FunpRepeat.of(Int.num(b), p(a));
			}).match("size.of .0", a -> {
				return FunpSizeOf.of(p(a));
			}).match(".0, .1", (a, b) -> {
				return FunpStruct.of(List.of(Pair.of("t0", p(a)), Pair.of("t1", p(b))));
			}).match("{ .0 }", a -> {
				return FunpStruct.of(Tree //
						.iter(a, Tree::decompose) //
						.map(this::kv) //
						.map(kv -> Pair.of(kv.t0, p(kv.t1))) //
						.toList());
			}).applyTree((op, l, r) -> {
				return FunpTree.of(op, p(l), p(r));
			}).applyIf(Atom.class, atom -> {
				var var = atom.name;
				return variables.contains(var) ? FunpVariable.of(var) : FunpVariableNew.of(var);
			}).nonNullResult();
		}

		private Funp consult(String url) {
			Fun<ReadStream, Funp> r0 = is -> is.doReader(isr -> FunpPredefine.of(parse(Suite.parse(To.string(isr)))));

			Fun<SourceEx<ReadStream, IOException>, Funp> r1 = source -> rethrow(() -> source.source()).doRead(r0::apply);

			if (url.startsWith("file://"))
				return r1.apply(() -> FileUtil.in(url.substring(7)));
			else if (url.startsWith("http://") || url.startsWith("https://"))
				return r0.apply(HttpUtil.get(url).inputStream());
			else
				return r1.apply(() -> ReadStream.of(getClass().getResourceAsStream(url)));
		}

		private Pair<String, Node> kv(Node n) {
			Node[] m;
			if ((m = Suite.pattern(".0 := .1").match(n)) != null || (m = Suite.pattern(".0: .1").match(n)) != null)
				return Pair.of(Atom.name(m[0]), m[1]);
			else
				return Pair.of(Atom.name(n), n);
		}

		private boolean isVar(Node v) {
			return v != dontCare && v instanceof Atom;
		}

		private Funp bind(Node a, Node b, Node c) {
			return bind(a, b, c, Suite.parse("error"));
		}

		private Funp bind(Node a, Node b, Node c, Node d) {
			var varsMutable = Mutable.of(ISet.<String> empty());

			Iterate<Funp> iter = be -> inspect.rewrite(be, Funp.class, n_ -> new Switch<Funp>(n_) //
					.applyIf(FunpVariableNew.class, f -> f.apply(var -> {
						varsMutable.update(varsMutable.get().replace(var));
						return FunpVariable.of(var);
					})).result());

			var be = iter.apply(p(a));
			var vars = varsMutable.get();
			var value = p(b);
			var then = new Parse(vars.streamlet().fold(variables, ISet::add)).p(c);
			var else_ = p(d);
			var f0 = new Bind(vars).bind(be, value, then, else_);
			var f1 = FunpCheckType.of(be, value, f0);
			return vars.streamlet().<Funp> fold(f1, (f, var) -> FunpDefine.of(Fdt.MONO, var, FunpDontCare.of(), f));
		}

		private Parse nv(String var) {
			return new Parse(variables.add(var));
		}
	}

	private class Bind {
		private ISet<String> variables;

		private Bind(ISet<String> variables) {
			this.variables = variables;
		}

		private Funp bind(Funp be, Funp value, Funp then, Funp else_) {
			IntObj_Obj<Int_Obj<Funp>, Funp> bindArray = (size0, fun0) -> {
				var fun1 = new Switch<Int_Obj<Funp>>(value //
				).applyIf(FunpArray.class, g -> {
					var elements = g.elements;
					return size0 == elements.size() ? elements::get : null;
				}).applyIf(FunpRepeat.class, g -> g.apply((count, expr) -> {
					Int_Obj<Funp> fun_ = i -> expr;
					return size0 == count ? fun_ : null;
				})).applyIf(Funp.class, g -> {
					return i -> FunpIndex.of(FunpReference.of(value), FunpNumber.ofNumber(i));
				}).result();

				return Ints_ //
						.range(size0) //
						.fold(then, (i, then_) -> bind(fun0.apply(i), fun1.apply(i), then_, else_));
			};

			if (be instanceof FunpBoolean && value instanceof FunpBoolean)
				return ((FunpBoolean) be).b == ((FunpBoolean) value).b ? then : else_;
			else if (be instanceof FunpNumber && value instanceof FunpNumber)
				return ((FunpNumber) be).i == ((FunpNumber) value).i ? then : else_;
			else {
				var result = be.sw( //
				).applyIf(FunpArray.class, f -> f.apply(elements0 -> {
					return bindArray.apply(elements0.size(), elements0::get);
				})).applyIf(FunpDontCare.class, f -> {
					return then;
				}).applyIf(FunpReference.class, f -> f.apply(expr -> {
					return bind(expr, FunpDeref.of(value), then, else_);
				})).applyIf(FunpRepeat.class, f -> f.apply((size0, expr0) -> {
					return bindArray.apply(size0, i -> expr0);
				})).applyIf(FunpStruct.class, f -> f.apply(pairs0 -> {
					var pairs1 = new Switch<List<Pair<String, Funp>>>(value).applyIf(FunpStruct.class, g -> g.pairs).result();
					var size0 = pairs0.size();

					Int_Obj<Funp> fun = pairs1 != null && size0 == pairs1.size() //
							? i -> pairs1.get(i).t1 //
							: i -> FunpField.of(FunpReference.of(value), pairs0.get(i).t0);

					return Ints_ //
							.range(size0) //
							.fold(then, (i, then_) -> bind(pairs0.get(i).t1, fun.apply(i), then_, else_));
				})).applyIf(FunpVariable.class, f -> f.apply(var -> {
					return variables.contains(var) ? FunpIoAssignRef.of(FunpReference.of(f), value, then) : be;
				})).result();

				return result != null ? result : FunpIf.of(FunpTree.of(TermOp.EQUAL_, be, value), then, else_);
			}
		}
	}

}
