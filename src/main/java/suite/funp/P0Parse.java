package suite.funp;

import static suite.util.Friends.fail;
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
			}

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
				var vn = a instanceof Atom ? Atom.name(a) : null;
				var m = Suite.pattern("[.0]").match(b);
				var isIndex = m != null && 0 < m.length && !isList(m[0]);
				return vn != null && variables.contains(vn) && !isIndex ? FunpApply.of(p(b), p(a)) : null;
			}).match("[.0]", a -> {
				return isList(a) ? FunpArray.of(Tree.iter(a).map(this::p).toList()) : null;
			}).match(Atom.FALSE, () -> {
				return FunpBoolean.of(false);
			}).match(Atom.TRUE, () -> {
				return FunpBoolean.of(true);
			}).match("type .0 = .1 ~ .2", (a, b, c) -> {
				return FunpCheckType.of(p(a), p(b), p(c));
			}).match("byte", () -> {
				return FunpCoerce.of(Coerce.BYTE, FunpDontCare.of());
			}).match("byte .0", a -> {
				return FunpCoerce.of(Coerce.BYTE, FunpNumber.ofNumber(num(a)));
			}).match("coerce.byte .0", a -> {
				return FunpCoerce.of(Coerce.BYTE, p(a));
			}).match("coerce.number .0", a -> {
				return FunpCoerce.of(Coerce.NUMBER, p(a));
			}).match("coerce.pointer .0", a -> {
				return FunpCoerce.of(Coerce.POINTER, p(a));
			}).match("consult .0", a -> {
				return consult(Str.str(a));
			}).match("define .0 := .1 ~ .2", (a, b, c) -> {
				return define(Fdt.POLY, a, p(b), c);
				// return parse(Suite.subst("poly .1 | (.0 => .2)", m));
			}).match("define .0 .1 := .2 ~ .3", (a, b, c, d) -> {
				return define(Fdt.POLY, a, lambda(b, c), d);
			}).match("let .0 := .1 ~ .2", (a, b, c) -> {
				return define(Fdt.MONO, a, p(b), c);
				// return parse(Suite.subst(".1 | (.0 => .2)", m));
			}).match("let.global .0 := .1 ~ .2", (a, b, c) -> {
				return define(Fdt.GLOB, a, p(b), c);
			}).match("define { .0 } ~ .1", (a, b) -> {
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
			}).match("case || .0", a -> {
				return new Object() {
					private Funp d(Node n) {
						Node[] m;
						if ((m = Suite.pattern(".0 => .1 || .2").match(n)) != null)
							return FunpIf.of(p(m[0]), p(m[1]), d(m[2]));
						else
							return p(n);
					}
				}.d(a);
			}).match(".0 [.1]", (a, b) -> {
				return !isList(b) ? FunpIndex.of(FunpReference.of(p(a)), p(b)) : null;
			}).match("io .0", a -> {
				return FunpIo.of(p(a));
			}).match("io.asm .0 {.1}", (a, b) -> {
				return FunpIoAsm.of(Tree.iter(a, TermOp.OR____).map(n -> {
					var ma = Suite.pattern(".0 = .1").match(n);
					return Pair.of(Amd64.me.regByName.get(ma[0]), p(ma[1]));
				}).toList(), Tree.iter(b, TermOp.OR____).toList());
			}).match("io.assign ^.0 := .1 ~ .2", (a, b, c) -> {
				return FunpIoAssignRef.of(FunpReference.of(p(a)), p(b), p(c));
			}).match("io.assign .0 := .1 ~ .2", (a, b, c) -> {
				return FunpIoAssignRef.of(FunpReference.of(FunpVariable.of(Atom.name(a))), p(b), p(c));
			}).match("io.let .0 := .1 ~ .2", (a, b, c) -> {
				return FunpApply.of(p(b), FunpIoCat.of(lambda(a, c)));
			}).match("io.perform .0 ~ .1", (a, b) -> {
				return FunpApply.of(p(a), FunpIoCat.of(lambda(dontCare, b)));
			}).match("io.fold .0 .1 .2", (a, b, c) -> {
				return FunpIoFold.of(p(a), p(b), p(c));
			}).match("io.for (.0 = .1; .2; .3)", (a, b, c, d) -> {
				var fold = FunpIoFold.of(p(b), lambda(a, c), lambda(a, d));
				return FunpApply.of(fold, FunpIoCat.of(p(Suite.parse("_ => io {}"))));
			}).match("io.map .0", a -> {
				return FunpIoMap.of(p(a));
			}).match(".0 => .1", (a, b) -> {
				return lambda(a, b);
			}).match("number", () -> {
				return FunpNumber.of(IntMutable.nil());
			}).match("number .0", a -> {
				return FunpNumber.ofNumber(num(a));
			}).applyIf(Int.class, n -> {
				return FunpNumber.ofNumber(n.number);
			}).match("predef .0", a -> {
				return FunpPredefine.of(p(a));
			}).match("address .0", a -> {
				return FunpReference.of(p(a));
			}).applyIf(Str.class, str -> {
				var vn = "s$" + Util.temp();
				var fa = FunpArray.of(To //
						.chars(str.value) //
						.streamlet() //
						.<Funp> map(ch -> FunpCoerce.of(Coerce.BYTE, FunpNumber.ofNumber(ch))) //
						.snoc(FunpCoerce.of(Coerce.BYTE, FunpNumber.ofNumber(0))) //
						.toList());
				return FunpDefine.of(Fdt.GLOB, vn, fa, FunpReference.of(FunpVariable.of(vn)));
			}).match("array .0 * .1", (a, b) -> {
				return FunpRepeat.of(b != Atom.of("_") ? Int.num(b) : -1, p(a));
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
				var vn = atom.name;
				return variables.contains(vn) ? FunpVariable.of(vn) : FunpVariableNew.of(vn);
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

		private Funp define(Fdt t, Node var, Funp value, Node expr) {
			var vn = isVar(var) ? Atom.name(var) : null;
			return vn != null ? FunpDefine.of(t, vn, value, nv(vn).p(expr)) : null;
		}

		private Pair<String, Node> kv(Node n) {
			Node[] m;
			if ((m = Suite.pattern(".0 .1 := .2").match(n)) != null)
				return Pair.of(Atom.name(m[0]), Suite.substitute(".0 => .1", m[1], m[2]));
			else if ((m = Suite.pattern(".0 := .1").match(n)) != null || (m = Suite.pattern(".0: .1").match(n)) != null)
				return Pair.of(Atom.name(m[0]), m[1]);
			else
				return Pair.of(Atom.name(n), n);
		}

		private Funp lambda(Node a, Node b) {
			String vn;
			Funp f;
			if (isVar(a))
				f = nv(vn = Atom.name(a)).p(b);
			else {
				vn = "l$" + Util.temp();
				f = nv(vn).bind(a, Atom.of(vn), b);
			}
			return FunpLambda.of(vn, f);
		}

		private int num(Node a) {
			var s = a instanceof Atom ? Atom.name(a) : null;
			if (s != null)
				return s.length() == 1 ? s.charAt(0) : fail();
			else if (a instanceof Int)
				return Int.num(a);
			else
				return fail();
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

		private boolean isList(Node l) {
			var tree = Tree.decompose(l, TermOp.AND___);
			return l == Atom.NIL || tree != null && isList(tree.getRight());
		}

		private boolean isVar(Node v) {
			return v != dontCare && v instanceof Atom;
		}

		private Parse nv(String var) {
			return new Parse(variables.replace(var));
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
