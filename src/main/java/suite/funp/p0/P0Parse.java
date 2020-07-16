package suite.funp.p0;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import primal.MoreVerbs.Read;
import primal.Verbs.Get;
import primal.adt.Mutable;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Iterate;
import primal.fp.Funs.Source;
import primal.persistent.PerMap;
import primal.persistent.PerSet;
import primal.primitive.adt.IntMutable;
import primal.streamlet.Streamlet2;
import suite.Suite;
import suite.assembler.Amd64;
import suite.funp.FunpCfg;
import suite.funp.Funp_;
import suite.funp.Funp_.Funp;
import suite.funp.P0.Coerce;
import suite.funp.P0.Fct;
import suite.funp.P0.Fdt;
import suite.funp.P0.FunpAdjustArrayPointer;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpCoerce;
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
import suite.funp.P0.FunpLambdaFree;
import suite.funp.P0.FunpMe;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPredefine;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpRemark;
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
import suite.streamlet.ReadChars;

public class P0Parse extends FunpCfg {

	private Atom dontCare = Atom.of("_");
	private Inspect inspect = Singleton.me.inspect;
	private String doToken = "$do";

	private int tagId;
	private Map<String, Integer> idByTag = new HashMap<>();

	public P0Parse(Funp_ f) {
		super(f);
	}

	public Funp parse(Node node) {
		return parse(node, PerMap.empty());
	}

	private Funp parse(Node node0, PerMap<Prototype, Node[]> macros) {
		var node1 = new P00Consult(this).c(node0);
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
				var lambda = bind(Fdt.L_MONO).lambda(dontCare, b);
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
					var lambda = bind(Fdt.L_MONO).lambda(dontCare, c);
					return checkDo(() -> FunpDefine.of(lambda.vn, apply, lambda.expr, Fdt.L_IOAP));
				} else
					return null;
			}).match(".0 => capture .1", (a, b) -> {
				return capture(bind(Fdt.L_MONO).lambdaSeparate(a, b), Fct.MANUAL);
			}).match(".0 => capture1 .1", (a, b) -> {
				return capture(bind(Fdt.L_MONO).lambdaSeparate(a, b), Fct.ONCE__);
			}).match(".0 => .1", (a, b) -> {
				return bind(Fdt.L_MONO).lambdaSeparate(a, b);
			}).match(".0 | .1", (a, b) -> {
				return FunpApply.of(p(a), p(b));
			}).match(".0 [.1]", (a, b) -> {
				return !isList(b) ? FunpIndex.of(FunpReference.of(p(a)), p(b)) : null;
			}).match(".0 ++ .1", (a, b) -> {
				return FunpAdjustArrayPointer.of(p(a), p(b));
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
				var isCompleted = Tree.decompose(a, TermOp.AND___) == null;
				return FunpStruct.of(isCompleted, kvs(a).mapValue(this::p).toList());
			}).match("!asm .0 {.1}/.2", (a, b, c) -> {
				return checkDo(() -> FunpDoAsm.of(Tree.read(a, TermOp.OR____).map(n -> {
					var ma = Suite.pattern(".0 = .1").match(n);
					return Pair.of(Amd64.me.regByName.get(ma[0]), p(ma[1]));
				}).toList(), Tree.read(b, TermOp.OR____).toList(), Amd64.me.regByName.get(c)));
			}).match("!assign .0 := .1 ~ .2", (a, b, c) -> {
				return checkDo(() -> FunpDoAssignRef.of(FunpReference.of(p(a)), p(b), p(c)));
			}).match("!delete^ .0 ~ .1", (a, b) -> {
				return checkDo(() -> FunpDoHeapDel.of(false, p(a), p(b)));
			}).match("!deletes^ .0 ~ .1", (a, b) -> {
				return checkDo(() -> FunpDoHeapDel.of(true, p(a), p(b)));
			}).match("!new^ .0", a -> {
				return new_(a, false);
			}).match("!news^ .0", a -> {
				return new_(a, true);
			}).match("address.of .0", a -> {
				return FunpReference.of(p(a));
			}).match("address.of.any", () -> {
				return FunpReference.of(FunpDontCare.of());
			}).match("array .0 * .1", (a, b) -> {
				return FunpRepeat.of(Int.num(a), p(b));
			}).match("array.of.many .0", a -> {
				return FunpRepeat.of(null, p(a));
			}).match("assert .0 ~ .1", (a, b) -> {
				return FunpIf.of(p(a), p(b), FunpError.of());
			}).match("boolean", () -> {
				return FunpBoolean.of(false);
			}).match("byte", () -> {
				return FunpCoerce.of(Coerce.NUMBER, Coerce.BYTE, FunpDontCare.of());
			}).match("byte .0", a -> {
				return FunpCoerce.of(Coerce.NUMBER, Coerce.BYTE, FunpNumber.ofNumber(num(a)));
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
					var bind = bind(Fdt.L_POLY);
					var lambda = bind.lambda(a, c);
					return FunpDefine.of(lambda.vn, p(b), lambda.expr, bind.outerFdt);
				} else
					return null;
				// return parse(Suite.subst("poly .1 | (.0 => .2)", m));
			}).match("define .0 .1 := .2 ~ .3", (a, b, c, d) -> {
				return define(a, bind(Fdt.L_MONO).lambdaSeparate(b, c), d, Fdt.L_POLY);
			}).match("define { .0 } ~ .1", (a, b) -> {
				return defineList(a, b, Fdt.L_POLY);
			}).match("define.function .0 .1 := .2 ~ .3", (a, b, c, d) -> {
				var lambda = bind(Fdt.L_MONO).lambdaSeparate(b, c);
				lambda.fct = Fct.STACKF;
				return define(a, lambda, d, Fdt.S_POLY);
			}).match("define.global .0 := .1 ~ .2", (a, b, c) -> {
				var tree = Tree.decompose(a, TermOp.TUPLE_);
				if (tree == null || !isId(tree.getLeft())) {
					var bind = bind(Fdt.G_POLY);
					var lambda = bind.lambda(a, c);
					return FunpDefine.of(lambda.vn, p(b), lambda.expr, bind.outerFdt);
				} else
					return null;
			}).match("define.global .0 .1 := .2 ~ .3", (a, b, c, d) -> {
				return define(a, bind(Fdt.L_MONO).lambdaSeparate(b, c), d, Fdt.G_POLY);
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
			}).match("glob (.0 => .1)", (a, b) -> { // without frame
				var lambda = bind(Fdt.L_MONO).lambdaSeparate(a, b);
				lambda.fct = Fct.NOSCOP;
				return lambda;
			}).match("if (`.0` = .1) then .2 else .3", (a, b, c, d) -> {
				return bind(Fdt.L_MONO).bind(a, b, c, d);
			}).match("if .0 then .1 else .2", (a, b, c) -> {
				return FunpIf.of(p(a), p(b), p(c));
			}).match("let .0 := .1 ~ .2", (a, b, c) -> {
				var tree = Tree.decompose(a, TermOp.TUPLE_);
				if (tree == null || !isId(tree.getLeft())) {
					var bind = bind(Fdt.L_MONO);
					var lambda = bind.lambda(a, c);
					return FunpDefine.of(lambda.vn, p(b), lambda.expr, bind.outerFdt);
				} else
					return null;
				// return parse(Suite.subst(".1 | (.0 => .2)", m));
			}).match("let .0 .1 := .2 ~ .3", (a, b, c, d) -> {
				return define(a, bind(Fdt.L_MONO).lambdaSeparate(b, c), d, Fdt.L_MONO);
			}).match("let { .0 } ~ .1", (a, b) -> {
				return defineList(a, b, Fdt.L_MONO);
			}).match("let.global .0 := .1 ~ .2", (a, b, c) -> {
				var tree = Tree.decompose(a, TermOp.TUPLE_);
				if (tree == null || !isId(tree.getLeft())) {
					var bind = bind(Fdt.G_MONO);
					var lambda = bind.lambda(a, c);
					return FunpDefine.of(lambda.vn, p(b), lambda.expr, bind.outerFdt);
				} else
					return null;
			}).match("let.global .0 .1 := .2 ~ .3", (a, b, c, d) -> {
				return define(a, bind(Fdt.L_MONO).lambdaSeparate(b, c), d, Fdt.G_MONO);
			}).match("let.global { .0 } ~ .1", (a, b) -> {
				return defineList(a, b, Fdt.G_MONO);
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
				return FunpPredefine.of("predefine$" + Get.temp(), p(a));
			}).match("predef/.0 .1", (a, b) -> {
				return FunpPredefine.of(Atom.name(a), p(b));
			}).match("size.of .0", a -> {
				return FunpSizeOf.of(p(a));
			}).match("sum .0 .1", (a, b) -> {
				return FunpTree.of(TermOp.PLUS__, p(a), p(b), null);
			}).match("type .0 .1", (a, b) -> {
				return FunpTypeCheck.of(p(a), null, p(b));
			}).match("type .0 = .1 ~ .2", (a, b, c) -> {
				return FunpTypeCheck.of(p(a), p(b), p(c));
			}).match("uncapture .0 ~ .1", (a, b) -> {
				return FunpLambdaFree.of(p(a), p(b));
			}).match(Atom.FALSE, () -> {
				return FunpBoolean.of(false);
			}).match(Atom.NIL, () -> {
				return FunpStruct.of(true, List.of());
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
				var vn = "s$" + Get.temp();
				var s = str.value;
				var fa = FunpArray.of(ReadChars //
						.from(s) //
						.<Funp> map(ch -> FunpCoerce.of(Coerce.NUMBER, Coerce.BYTE, FunpNumber.ofNumber(ch))) //
						.snoc(FunpCoerce.of(Coerce.NUMBER, Coerce.BYTE, FunpNumber.ofNumber(0))) //
						.toList());
				var fr = s.length() < 80 ? FunpRemark.of(s, fa) : fa;
				return FunpDefine.of(vn, fr, FunpReference.of(FunpVariable.of(vn)), Fdt.G_MONO);
			}).applyTree((op, l, r) -> {
				if (op == TermOp.TUPLE_) {
					var apply = FunpApply.of(p(r), p(l));
					return checkDo() && isBang(l) ? FunpDoEvalIo.of(apply) : apply;
				} else
					return FunpTree.of(op, p(l), p(r));
			}).nonNullResult();
		}

		private FunpLambda capture(FunpLambda lambda, Fct fct) {
			lambda.fct = fct;
			return lambda;
		}

		private <T> T checkDo(Source<T> source) {
			return checkDo() ? source.g() : Funp_.fail(null, "do block required");
		}

		private boolean checkDo() {
			return vns.contains(doToken);
		}

		private Funp define(Node var, Funp value, Node expr, Fdt t) {
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
			var lf = bind(Fdt.L_MONO).lambda(a, true);
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
			Tree t;
			while ((t = Tree.decompose(n, TermOp.ITEM__)) != null)
				n = t.getRight();
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
					.read(node, t -> {
						var tree = Tree.decompose(t);
						if (tree != null) {
							var operator = tree.getOperator();
							var b = operator == TermOp.AND___ || operator == TermOp.CONTD_;
							return b ? tree : Funp_.fail(null, "unknown operator " + operator);
						} else
							return null;
					}) //
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

		private Funp new_(Node a, boolean isDynamicSize) {
			var n = FunpDoHeapNew.of(isDynamicSize, null);
			return checkDo(() -> {
				if (a == dontCare)
					return n;
				else {
					var vn = "n$" + Get.temp();
					var v = FunpVariable.of(vn);
					var ref = FunpReference.of(FunpDeref.of(v));
					return FunpDefine.of(vn, n, FunpDoAssignRef.of(ref, p(a), v), Fdt.L_MONO);
				}
			});
		}

		private int num(Node a) {
			var s = a instanceof Atom ? Atom.name(a) : null;
			if (s != null)
				return s.length() == 1 ? s.charAt(0) : Funp_.fail(null, "'" + a + "' is not a number");
			else if (a instanceof Int)
				return Int.num(a);
			else
				return Funp_.fail(null, "'" + a + "' is not a number");
		}

		private Bind bind(Fdt fdt) {
			var bind = new Bind();
			bind.fdt = fdt;
			return bind;
		}

		private class Bind {
			private Fdt fdt;
			private Fdt outerFdt;

			private FunpLambda lambdaSeparate(Node a, Node b) {
				return lambda(a, false).apply(b);
			}

			private FunpLambda lambda(Node a, Node b) {
				return lambda(a, true).apply(b);
			}

			private Fun<Node, FunpLambda> lambda(Node a, boolean isPassDo) {
				var isVar = isVar(a);
				var vn = isVar ? Atom.name(a) : "l$" + Get.temp();
				outerFdt = isVar ? fdt : Fdt.L_MONO;
				var nv = isPassDo ? nv(vn) : new Parse(vns.replace(vn).remove(doToken));
				return b -> {
					var f = isVar ? nv.p(b) : nv.bind(fdt).bind(a, Atom.of(vn), b);
					return FunpLambda.of(vn, f);
				};
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
				return vns_.streamlet().<Funp> fold(f1, (f, vn) -> FunpDefine.of(vn, FunpDontCare.of(), f, fdt));
			}
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
