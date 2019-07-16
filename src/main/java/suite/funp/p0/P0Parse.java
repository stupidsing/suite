package suite.funp.p0;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.Suite;
import suite.adt.Mutable;
import suite.adt.pair.Pair;
import suite.assembler.Amd64;
import suite.funp.Funp_;
import suite.funp.Funp_.Funp;
import suite.funp.P0.Fdt;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpCoerce;
import suite.funp.P0.FunpCoerce.Coerce;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpDoAsm;
import suite.funp.P0.FunpDoAssignRef;
import suite.funp.P0.FunpDoAssignVar;
import suite.funp.P0.FunpDoEvalIo;
import suite.funp.P0.FunpDoHeapDel;
import suite.funp.P0.FunpDoHeapNew;
import suite.funp.P0.FunpDoWhile;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpError;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpIo;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpMe;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPredefine;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpRepeat;
import suite.funp.P0.FunpSizeOf;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpTag;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpTree2;
import suite.funp.P0.FunpTypeCheck;
import suite.funp.P0.FunpVariable;
import suite.funp.P0.FunpVariableNew;
import suite.inspect.Inspect;
import suite.lp.kb.Prototype;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.SwitchNode;
import suite.node.io.TermOp;
import suite.node.util.Singleton;
import suite.node.util.TreeUtil;
import suite.persistent.PerMap;
import suite.persistent.PerSet;
import suite.primitive.IntMutable;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.util.To;
import suite.util.Util;

public class P0Parse {

	private Atom dontCare = Atom.of("_");
	private Inspect inspect = Singleton.me.inspect;
	private String doToken = "$do";

	private int tagId;
	private Map<String, Integer> idByTag = new HashMap<>();

	public Funp parse(Node node) {
		return parse(node, PerMap.empty());
	}

	private Funp parse(Node node0, PerMap<Prototype, Node[]> macros) {
		var node1 = new P00Consult().c(node0);
		var node2 = new P01Expand(macros).e(node1);
		return new Parse(PerSet.empty()).p(node2);
	}

	private class Parse {
		private PerSet<String> vns;

		private Parse(PerSet<String> vns) {
			this.vns = vns;
		}

		private Funp p(Node node) {
			return new SwitchNode<Funp>(node //
			).match("!! .0", a -> {
				return checkDo(() -> FunpDoEvalIo.of(p(a)));
			}).match("!! .0 ~ .1", (a, b) -> {
				var lambda = lambda(dontCare, b);
				return checkDo(() -> FunpDefine.of(lambda.vn, p(a), lambda.expr, Fdt.L_IOAP));
			}).match(".0:.1 .2", (a, b, c) -> {
				var c0 = Coerce.valueOf(Atom.name(b).toUpperCase());
				var c1 = Coerce.valueOf(Atom.name(a).toUpperCase());
				return FunpCoerce.of(c0, c1, p(c));
			}).match(".0 .1 .2", (a, b, c) -> {
				if (TreeUtil.tupleOperations.containsKey(b))
					return FunpTree2.of((Atom) b, p(a), p(c));
				else
					return null;
			}).match(".0 .1 ~ .2", (a, b, c) -> {
				if (isBang(a)) {
					var apply = FunpApply.of(p(b), p(a));
					var lambda = lambda(dontCare, c);
					return checkDo(() -> FunpDefine.of(lambda.vn, apply, lambda.expr, Fdt.L_IOAP));
				} else
					return null;
			}).match(".0 => .1", (a, b) -> {
				return lambdaSeparate(a, b);
			}).match(".0 | .1", (a, b) -> {
				return FunpApply.of(p(a), p(b));
			}).match(".0 [.1]", (a, b) -> {
				return !isList(b) ? FunpIndex.of(FunpReference.of(p(a)), p(b)) : null;
			}).match(".0, .1", (a, b) -> {
				return FunpStruct.of(List.of(Pair.of("t0", p(a)), Pair.of("t1", p(b))));
			}).match(".0:.1", (a, b) -> {
				var tag = Atom.name(a);
				return FunpTag.of(IntMutable.of(idByTag.computeIfAbsent(tag, t -> ++tagId)), tag, p(b));
			}).match(".0/.1", (a, b) -> {
				return b instanceof Atom ? FunpField.of(FunpReference.of(p(a)), Atom.name(b)) : null;
			}).match(".0*", a -> {
				return FunpDeref.of(p(a));
			}).match("[.0]", a -> {
				return isList(a) ? FunpArray.of(Tree.read(a).map(this::p).toList()) : null;
			}).match("{ .0 }", a -> {
				return FunpStruct.of(kvs(a).mapValue(this::p).toList());
			}).match("!asm .0 {.1}/.2", (a, b, c) -> {
				return checkDo(() -> FunpDoAsm.of(Tree.read(a, TermOp.OR____).map(n -> {
					var ma = Suite.pattern(".0 = .1").match(n);
					return Pair.of(Amd64.me.regByName.get(ma[0]), p(ma[1]));
				}).toList(), Tree.read(b, TermOp.OR____).toList(), Amd64.me.regByName.get(c)));
			}).match("!assign .0 := .1 ~ .2", (a, b, c) -> {
				return checkDo(() -> FunpDoAssignRef.of(FunpReference.of(p(a)), p(b), p(c)));
			}).match("!delete^ .0 ~ .1", (a, b) -> {
				return checkDo(() -> FunpDoHeapDel.of(p(a), p(b)));
			}).match("!new^ .0", a -> {
				var n = FunpDoHeapNew.of();
				return checkDo(() -> {
					if (a == dontCare)
						return n;
					else {
						var vn = "n$" + Util.temp();
						var v = FunpVariable.of(vn);
						var ref = FunpReference.of(FunpDeref.of(v));
						return FunpDefine.of(vn, n, FunpDoAssignRef.of(ref, p(a), v), Fdt.L_MONO);
					}
				});
			}).match("address.of .0", a -> {
				return FunpReference.of(p(a));
			}).match("address.of.any", () -> {
				return FunpReference.of(FunpDontCare.of());
			}).match("array .0 * .1", (a, b) -> {
				return FunpRepeat.of(a != dontCare ? Int.num(a) : null, p(b));
			}).match("assert .0 ~ .1", (a, b) -> {
				return FunpIf.of(p(a), p(b), FunpError.of());
			}).match("byte", () -> {
				return FunpCoerce.of(Coerce.NUMBER, Coerce.BYTE, FunpDontCare.of());
			}).match("byte .0", a -> {
				return FunpCoerce.of(Coerce.NUMBER, Coerce.BYTE, FunpNumber.ofNumber(num(a)));
			}).match("capture (.0 => .1)", (a, b) -> {
				return capture(lambdaSeparate(a, b));
			}).match("case || .0", a -> {
				return new Object() {
					private Funp d(Node n) {
						var m = Suite.pattern(".0 => .1 || .2").match(n);
						return m != null ? FunpIf.of(p(m[0]), p(m[1]), d(m[2])) : p(n);
					}
				}.d(a);
			}).match("define .0 := .1 ~ .2", (a, b, c) -> {
				var tree = Tree.decompose(a, TermOp.TUPLE_);
				if (tree == null || !isId(tree.getLeft())) {
					var lambda = lambda(a, c);
					return FunpDefine.of(lambda.vn, p(b), lambda.expr, Fdt.L_POLY);
				} else
					return null;
				// return parse(Suite.subst("poly .1 | (.0 => .2)", m));
			}).match("define .0 .1 := .2 ~ .3", (a, b, c, d) -> {
				return define(Fdt.L_POLY, a, lambdaSeparate(b, c), d);
			}).match("define { .0 } ~ .1", (a, b) -> {
				return defineList(a, b, Fdt.L_POLY);
			}).match("define.global .0 .1 := .2 ~ .3", (a, b, c, d) -> {
				return define(Fdt.G_POLY, a, lambdaSeparate(b, c), d);
			}).match("define.global { .0 } ~ .1", (a, b) -> {
				return defineList(a, b, Fdt.G_POLY);
			}).match("define.virtual .0 := .1 ~ .2", (a, b, c) -> {
				return defineList(Read.each2(Pair.of(Atom.name(a), b)), c, Fdt.VIRT);
			}).match("do! .0", a -> {
				return do_(parse -> parse.p(a));
			}).match("error", () -> {
				return FunpError.of();
			}).match("fold (.0 := .1 # .2 # .3 # .4)", (a, b, c, d, e) -> {
				return fold(a, b, c, d, e);
			}).match("for! (.0 := .1 # .2 # .3 # .4)", (a, b, c, d, e) -> {
				return do_(parse -> parse.fold(a, b, c, d, e));
			}).match("if (`.0` = .1) then .2 else .3", (a, b, c, d) -> {
				return bind(a, b, c, d);
			}).match("if .0 then .1 else .2", (a, b, c) -> {
				return FunpIf.of(p(a), p(b), p(c));
			}).match("let .0 := .1 ~ .2", (a, b, c) -> {
				var tree = Tree.decompose(a, TermOp.TUPLE_);
				if (tree == null || !isId(tree.getLeft())) {
					var lambda = lambda(a, c);
					return FunpDefine.of(lambda.vn, p(b), lambda.expr, Fdt.L_MONO);
				} else
					return null;
				// return parse(Suite.subst(".1 | (.0 => .2)", m));
			}).match("let .0 .1 := .2 ~ .3", (a, b, c, d) -> {
				return define(Fdt.L_MONO, a, lambdaSeparate(b, c), d);
			}).match("let { .0 } ~ .1", (a, b) -> {
				return defineList(a, b, Fdt.L_MONO);
			}).match("let.global .0 := .1 ~ .2", (a, b, c) -> {
				return define(Fdt.G_MONO, a, p(b), c);
			}).match("let.global .0 .1 := .2 ~ .3", (a, b, c, d) -> {
				return define(Fdt.G_MONO, a, lambdaSeparate(b, c), d);
			}).match("me", () -> {
				return FunpMe.of();
			}).match("null", () -> {
				return FunpCoerce.of(Coerce.NUMBER, Coerce.POINTER, FunpNumber.ofNumber(0));
			}).match("number", () -> {
				return FunpNumber.of(IntMutable.nil());
			}).match("number .0", a -> {
				return FunpNumber.ofNumber(num(a));
			}).match("numberp", () -> {
				return FunpCoerce.of(Coerce.NUMBER, Coerce.NUMBERP, FunpDontCare.of());
			}).match("numberp .0", a -> {
				return FunpCoerce.of(Coerce.NUMBER, Coerce.NUMBERP, FunpNumber.ofNumber(num(a)));
			}).match("predef .0", a -> {
				return FunpPredefine.of("predefine$" + Util.temp(), p(a));
			}).match("predef/.0 .1", (a, b) -> {
				return FunpPredefine.of(Atom.name(a), p(b));
			}).match("size.of .0", a -> {
				return FunpSizeOf.of(p(a));
			}).match("type .0 .1", (a, b) -> {
				return FunpTypeCheck.of(p(a), null, p(b));
			}).match("type .0 = .1 ~ .2", (a, b, c) -> {
				return FunpTypeCheck.of(p(a), p(b), p(c));
			}).match(Atom.FALSE, () -> {
				return FunpBoolean.of(false);
			}).match(Atom.NIL, () -> {
				return FunpStruct.of(List.of());
			}).match(Atom.TRUE, () -> {
				return FunpBoolean.of(true);
			}).match(dontCare, () -> {
				return FunpDontCare.of();
			}).applyIf(Atom.class, atom -> {
				var vn = atom.name;
				return vns.contains(vn) ? FunpVariable.of(vn) : FunpVariableNew.of(vn);
			}).applyIf(Int.class, n -> {
				return FunpNumber.ofNumber(n.number);
			}).applyIf(Str.class, str -> {
				var vn = "s$" + Util.temp();
				var fa = FunpArray.of(To //
						.chars(str.value) //
						.streamlet() //
						.<Funp> map(ch -> FunpCoerce.of(Coerce.NUMBER, Coerce.BYTE, FunpNumber.ofNumber(ch))) //
						.snoc(FunpCoerce.of(Coerce.NUMBER, Coerce.BYTE, FunpNumber.ofNumber(0))) //
						.toList());
				return FunpDefine.of(vn, fa, FunpReference.of(FunpVariable.of(vn)), Fdt.G_MONO);
			}).applyTree((op, l, r) -> {
				if (op == TermOp.TUPLE_) {
					var apply = FunpApply.of(p(r), p(l));
					return checkDo() && isBang(l) ? FunpDoEvalIo.of(apply) : apply;
				} else
					return FunpTree.of(op, p(l), p(r));
			}).nonNullResult();
		}

		private FunpLambda capture(FunpLambda lambda) {
			lambda.isCapture = true;
			return lambda;
		}

		private <T> T checkDo(Source<T> source) {
			return checkDo() ? source.g() : Funp_.fail(null, "do block required");
		}

		private boolean checkDo() {
			return vns.contains(doToken);
		}

		private Funp define(Fdt t, Node var, Funp value, Node expr) {
			var vn = isVar(var) ? Atom.name(var) : null;
			return vn != null ? FunpDefine.of(vn, value, nv(vn).p(expr), t) : null;
		}

		private Funp defineList(Node a, Node b, Fdt fdt) {
			return defineList(kvs(a).collect(), b, fdt);
		}

		private Funp defineList(Streamlet2<String, Node> list, Node b, Fdt fdt) {
			var p1 = new Parse(list.fold(vns, (vns, k, v) -> vns.add(k)));
			return FunpDefineRec.of(list.mapValue(p1::p).toList(), p1.p(b), fdt);
		}

		private Funp do_(Fun<Parse, Funp> f) {
			return !checkDo() ? FunpIo.of(f.apply(nv(doToken))) : Funp_.fail(null, "already in do block");
		}

		private Funp fold(Node a, Node b, Node c, Node d, Node e) {
			var lf = nv(doToken).lambda(a, true);
			var lc = lf.apply(c);
			var ld = lf.apply(d);
			var le = lf.apply(e);
			var vn = lc.vn;
			var var = FunpVariable.of(vn);
			var while_ = lc.expr;
			var do_ = FunpDoAssignVar.of(var, ld.expr, FunpDontCare.of());
			return FunpDefine.of( //
					vn, //
					p(b), //
					FunpDoWhile.of(while_, do_, le.expr), //
					Fdt.L_IOAP);
		}

		private boolean isBang(Node n) {
			return n instanceof Atom && Atom.name(n).startsWith("!");
		}

		private boolean isId(Node n) {
			if (n instanceof Atom) {
				var ch0 = Atom.name(n).charAt(0);
				return ch0 == '!' || Character.isAlphabetic(ch0);
			} else
				return false;
		}

		private Streamlet2<String, Node> kvs(Node node) {
			return Tree //
					.iter(node, Tree::decompose) //
					.map(n -> {
						Node[] m;
						if ((m = Suite.pattern(".0 .1 := .2").match(n)) != null)
							return Pair.of(Atom.name(m[0]), Suite.substitute(".0 => .1", m[1], m[2]));
						else if ((m = Suite.pattern(".0 := .1").match(n)) != null
								|| (m = Suite.pattern(".0: .1").match(n)) != null)
							return Pair.of(Atom.name(m[0]), m[1]);
						else
							return Pair.of(Atom.name(n), n);
					}) //
					.map2(Pair::fst, Pair::snd);
		}

		private FunpLambda lambdaSeparate(Node a, Node b) {
			return lambda(a, false).apply(b);
		}

		private FunpLambda lambda(Node a, Node b) {
			return lambda(a, true).apply(b);
		}

		private Fun<Node, FunpLambda> lambda(Node a, boolean isPassDo) {
			var isVar = isVar(a);
			var vn = isVar ? Atom.name(a) : "l$" + Util.temp();
			var nv = isPassDo ? nv(vn) : new Parse(vns.replace(vn).remove(doToken));
			return b -> {
				var f = isVar ? nv.p(b) : nv.bind(a, Atom.of(vn), b);
				return FunpLambda.of(vn, f, false);
			};
		}

		private int num(Node a) {
			var s = a instanceof Atom ? Atom.name(a) : null;
			if (s != null)
				return s.length() == 1 ? s.charAt(0) : Funp_.fail(null, "not a number");
			else if (a instanceof Int)
				return Int.num(a);
			else
				return Funp_.fail(null, "not a number");
		}

		private Funp bind(Node a, Node b, Node c) {
			return bind(a, b, c, Suite.parse("error"));
		}

		private Funp bind(Node a, Node b, Node c, Node d) {
			var vnsMutable = Mutable.of(PerSet.<String> empty());

			Iterate<Funp> iter = be -> inspect.rewrite(be, Funp.class,
					n_ -> n_.cast(FunpVariableNew.class, f -> f.apply(vn -> {
						vnsMutable.update(vnsMutable.value().replace(vn));
						return FunpVariable.of(vn);
					})));

			var be = iter.apply(p(a));
			var vns_ = vnsMutable.value();
			var value = p(b);
			var then = new Parse(vns_.streamlet().fold(vns, PerSet::add)).p(c);
			var else_ = p(d);
			var f0 = new P03Bind(vns_).bind(be, value, then, else_);
			var f1 = FunpTypeCheck.of(be, value, f0);
			return vns_.streamlet().<Funp> fold(f1, (f, vn) -> FunpDefine.of(vn, FunpDontCare.of(), f, Fdt.L_MONO));
		}

		private boolean isList(Node l) {
			return TreeUtil.isList(l, TermOp.AND___);
		}

		private boolean isVar(Node v) {
			return v != dontCare && v != Atom.NIL && v instanceof Atom;
		}

		private Parse nv(String vn) {
			return new Parse(vns.replace(vn));
		}
	}

}
