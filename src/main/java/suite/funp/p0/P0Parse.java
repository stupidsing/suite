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
import suite.assembler.Amd64;
import suite.funp.FunpCfg;
import suite.funp.FunpOp;
import suite.funp.Funp_;
import suite.funp.Funp_.Funp;
import suite.funp.P0.Coerce;
import suite.funp.P0.Fct;
import suite.funp.P0.Fdt;
import suite.funp.P0.Fpt;
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
import suite.funp.P0.FunpLog;
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
			return Funp_.<Funp> switchNode(node //
			).match("%0:%1 %2", (a, b, c) -> { // coerce: conversion between shorter and longer data types
				var c0 = Coerce.valueOf(Atom.name(b).toUpperCase());
				var c1 = Coerce.valueOf(Atom.name(a).toUpperCase());
				return FunpCoerce.of(c0, c1, p(c));
			}).match("%0 %1 %2", (a, b, c) -> { // invokes binary operator
				if (TreeUtil.tupleOperations.containsKey(b))
					return FunpTree2.of((Atom) b, p(a), p(c));
				else
					return null;
			}).match("%0 %1 ~ %2", (a, b, c) -> { // performs side-effect before
				if (isBang(a)) {
					var apply = FunpApply.of(p(b), p(a));
					var lambda = bind(Fdt.L_MONO).lambda(dontCare, c);
					return checkDo(() -> FunpDefine.of(lambda.vn, apply, lambda.expr, Fdt.L_IOAP));
				} else
					return null;
			}).match("%0 => capture %1", (a, b) -> { // capturing lambda that would be freed by uncapture
				return capture(bind(Fdt.L_MONO).lambdaSeparate(a, b), Fct.MANUAL);
			}).match("%0 => capture1 %1", (a, b) -> { // capturing lambda that would be freed by first invocation
				return capture(bind(Fdt.L_MONO).lambdaSeparate(a, b), Fct.ONCE__);
			}).match("%0 => %1", (a, b) -> { // lambda that only refer to parent stack frames - cannot be returned!
				return bind(Fdt.L_MONO).lambdaSeparate(a, b);
			}).match("%0 | !!", a -> { // unboxes an I/O
				return checkDo(() -> FunpDoEvalIo.of(p(a)));
			}).match("%0 | !! %1", (a, b) -> { // perform side-effect before
				var lambda = bind(Fdt.L_MONO).lambda(dontCare, b);
				return checkDo(() -> FunpDefine.of(lambda.vn, p(a), lambda.expr, Fdt.L_IOAP));
			}).match("%0 | defer %1", (a, b) -> { // defers closing by a function
				return checkDo(() -> FunpPredefine.of("defer$" + Get.temp(), p(a), Fpt.APPLY_, null, p(b)));
			}).match("%0 | defer/%1", (a, b) -> { // defers closing by a child function
				return checkDo(() -> FunpPredefine.of("defer$" + Get.temp(), p(a), Fpt.INVOKE, Atom.name(b), null));
			}).match("%0 | %1", (a, b) -> { // applies a function, pipe form
				return FunpApply.of(p(a), p(b));
			}).match("%0 [%1]", (a, b) -> { // indexes an array
				return !isArray(b) ? FunpIndex.of(FunpReference.of(p(a)), p(b)) : null;
			}).match("%0 ++ %1", (a, b) -> { // adjusts a pointer
				return FunpAdjustArrayPointer.of(p(a), p(b));
			}).match("%0, %1", (a, b) -> { // forms a tuple
				return FunpStruct.of(List.of(Pair.of("t0", p(a)), Pair.of("t1", p(b))));
			}).match("%0:%1", (a, b) -> { // forms a tag
				var tag = Atom.name(a);
				return FunpTag.of(IntMutable.of(idByTag.computeIfAbsent(tag, t -> ++tagId)), tag, p(b));
			}).match("%0.%1", (a, b) -> { // retrieves member of a struct
				return b instanceof Atom ? FunpField.of(FunpReference.of(p(a)), Atom.name(b)) : null;
			}).match("%0*", a -> { // dereferences a pointer
				return FunpDeref.of(p(a));
			}).match("[%0]", a -> { // forms a list
				return isArray(a) ? FunpArray.of(Tree.read(a).map(this::p).toList()) : null;
			}).match("{ %0 }", a -> { // forms a struct
				var isCompleted = Tree.decompose(a, FunpOp.AND___) == null;
				return FunpStruct.of(isCompleted, kvs(a).mapValue(this::p).toList());
			}).match("!asm %0 {%1}/%2", (a, b, c) -> {
				return checkDo(() -> FunpDoAsm.of(Tree.read(a, FunpOp.OR____).map(n -> {
					var ma = Funp_.pattern("%0 = %1").match(n);
					return Pair.of(Amd64.me.regByName.get(ma[0]), p(ma[1]));
				}).toList(), Tree.read(b, FunpOp.OR____).toList(), Amd64.me.regByName.get(c)));
			}).match("!assign %0 := %1 ~ %2", (a, b, c) -> { // re-assigns a variable
				return checkDo(() -> FunpDoAssignRef.of(FunpReference.of(p(a)), p(b), p(c)));
			}).match("!delete^ %0 ~ %1", (a, b) -> { // de-allocates
				return checkDo(() -> FunpDoHeapDel.of(false, p(a), p(b)));
			}).match("!delete-array^ %0 ~ %1", (a, b) -> { // de-allocates an array
				return checkDo(() -> FunpDoHeapDel.of(true, p(a), p(b)));
			}).match("!new^ %0", a -> { // allocates and assigns
				return new_(a, false, null);
			}).match("!new-array^ (%0 * %1)", (a, b) -> { // allocates a fixed-size array
				return new_(b, true, p(a));
			}).match("address-of %0", a -> { // gets a pointer to something
				return FunpReference.of(p(a));
			}).match("address-of-any", () -> { // gets a pointer to the void
				return FunpReference.of(FunpDontCare.of());
			}).match("array %0 * %1", (a, b) -> { // forms a array repeating an element
				return FunpRepeat.of(Int.num(a), p(b));
			}).match("array-of-many %0", a -> { // an virtual array; for deriving types, cannot be instantiated
				return FunpRepeat.of(null, p(a));
			}).match("assert %0 ~ %1", (a, b) -> {
				return FunpIf.of(p(a), p(b), FunpError.of("failed assert: " + a));
			}).match("boolean", () -> { // boolean type
				return FunpBoolean.of(false);
			}).match("byte", () -> { // byte type
				return FunpCoerce.of(Coerce.NUMBER, Coerce.BYTE, FunpDontCare.of());
			}).match("byte %0", a -> { // forms a byte
				return FunpCoerce.of(Coerce.NUMBER, Coerce.BYTE, FunpNumber.ofNumber(num(a)));
			}).match("case || %0", a -> { // select case
				return new Object() {
					private Funp d(Node n) {
						var m = Funp_.pattern("%0 => %1 || %2").match(n);
						return m != null ? FunpIf.of(p(m[0]), p(m[1]), d(m[2])) : p(n);
					}
				}.d(a);
			}).match("define %0 := %1 ~ %2", (a, b, c) -> { // defines a variable
				var tree = Tree.decompose(a, FunpOp.TUPLE_);
				if (tree == null || !isId(tree.getLeft())) {
					var bind = bind(Fdt.L_POLY);
					var lambda = bind.lambda(a, c);
					return FunpDefine.of(lambda.vn, p(b), lambda.expr, bind.outerFdt);
				} else
					return null;
				// return parse(Funp_.subst("poly %1 | (%0 => %2)", m));
			}).match("define %0 %1 := %2 ~ %3", (a, b, c, d) -> { // defines a function
				return define(a, bind(Fdt.L_MONO).lambdaSeparate(b, c), d, Fdt.L_POLY);
			}).match("define { %0 } ~ %1", (a, b) -> { // define lots of variables
				return defineList(a, b, Fdt.L_POLY);
			}).match("define-function %0 %1 := %2 ~ %3", (a, b, c, d) -> {
				var lambda = bind(Fdt.L_MONO).lambdaSeparate(b, c);
				lambda.fct = Fct.STACKF;
				return define(a, lambda, d, Fdt.S_POLY);
			}).match("define-global %0 := %1 ~ %2", (a, b, c) -> { // defines a global variable
				var tree = Tree.decompose(a, FunpOp.TUPLE_);
				if (tree == null || !isId(tree.getLeft())) {
					var bind = bind(Fdt.G_POLY);
					var lambda = bind.lambda(a, c);
					return FunpDefine.of(lambda.vn, p(b), lambda.expr, bind.outerFdt);
				} else
					return null;
			}).match("define-global %0 %1 := %2 ~ %3", (a, b, c, d) -> { // defines a global function
				return define(a, bind(Fdt.L_MONO).lambdaSeparate(b, c), d, Fdt.G_POLY);
			}).match("define-global { %0 } ~ %1", (a, b) -> { // define lots of global variables
				return defineList(a, b, Fdt.G_POLY);
			}).match("define-virtual %0 := %1 ~ %2", (a, b, c) -> { // defines a name for typing
				return defineList(Read.each2(Pair.of(Atom.name(a), b)), c, Fdt.VIRT);
			}).match("do! %0", a -> { // boxes an I/O operation
				return do_(parse -> parse.p(a));
			}).match("error", () -> { // throws up
				return FunpError.of("error");
			}).match("fold (%0 := %1 # %2 # %3 # %4)", (a, b, c, d, e) -> { // looping
				return fold(a, b, c, d, e);
			}).match("for! (%0 := %1 # %2 # %3 # %4)", (a, b, c, d, e) -> { // looping I/O operations
				return do_(parse -> parse.fold(a, b, c, d, e));
			}).match("glob (%0 => %1)", (a, b) -> { // without frame
				var lambda = bind(Fdt.L_MONO).lambdaSeparate(a, b);
				lambda.fct = Fct.NOSCOP;
				return lambda;
			}).match("if (`%0` = %1) then %2 else %3", (a, b, c, d) -> {
				return bind(Fdt.L_MONO).bind(a, b, c, d);
			}).match("if %0 then %1 else %2", (a, b, c) -> {
				return FunpIf.of(p(a), p(b), p(c));
			}).match("let %0 := %1 ~ %2", (a, b, c) -> { // defines a variable
				var tree = Tree.decompose(a, FunpOp.TUPLE_);
				if (tree == null || !isId(tree.getLeft())) {
					var bind = bind(Fdt.L_MONO);
					var lambda = bind.lambda(a, c);
					return FunpDefine.of(lambda.vn, p(b), lambda.expr, bind.outerFdt);
				} else
					return null;
				// return parse(Funp_.subst("%1 | (%0 => %2)", m));
			}).match("let %0 %1 := %2 ~ %3", (a, b, c, d) -> { // defines a function
				return define(a, bind(Fdt.L_MONO).lambdaSeparate(b, c), d, Fdt.L_MONO);
			}).match("let { %0 } ~ %1", (a, b) -> { // define lots of variables
				return defineList(a, b, Fdt.L_MONO);
			}).match("let-global %0 := %1 ~ %2", (a, b, c) -> { // defines a global variable
				var tree = Tree.decompose(a, FunpOp.TUPLE_);
				if (tree == null || !isId(tree.getLeft())) {
					var bind = bind(Fdt.G_MONO);
					var lambda = bind.lambda(a, c);
					return FunpDefine.of(lambda.vn, p(b), lambda.expr, bind.outerFdt);
				} else
					return null;
			}).match("let-global %0 %1 := %2 ~ %3", (a, b, c, d) -> { // defines a global function
				return define(a, bind(Fdt.L_MONO).lambdaSeparate(b, c), d, Fdt.G_MONO);
			}).match("let-global { %0 } ~ %1", (a, b) -> { // define lots of global functions
				return defineList(a, b, Fdt.G_MONO);
			}).match("log %0 ~ %1", (a, b) -> { // show a value to console
				return FunpLog.of(p(a), p(b));
			}).match("me", () -> { // this
				return FunpMe.of();
			}).match("null", () -> {
				return FunpCoerce.of(Coerce.NUMBER, Coerce.POINTER, FunpNumber.ofNumber(0));
			}).match("number", () -> { // a number type
				return FunpNumber.of(IntMutable.nil());
			}).match("number %0", a -> { // forms a number
				return FunpNumber.ofNumber(num(a));
			}).match("numberp", () -> { // a number type with the same size as a pointer
				return FunpCoerce.of(Coerce.NUMBER, Coerce.NUMBERP, FunpDontCare.of());
			}).match("numberp %0", a -> { // forms a number with the same size as a pointer
				return FunpCoerce.of(Coerce.NUMBER, Coerce.NUMBERP, FunpNumber.ofNumber(num(a)));
			}).match("precapture %0", a -> { // should be called defer.uncapture???
				return FunpPredefine.of("precapture$" + Get.temp(), p(a), Fpt.FREE__);
			}).match("predef %0", a -> { // defines a block as a separate variable; able to get a pointer to it
				return FunpPredefine.of("predefine$" + Get.temp(), p(a), Fpt.NONE__);
			}).match("predef/%0 %1", (a, b) -> { // defines a block as a separate named variable
				return FunpPredefine.of(Atom.name(a), p(b), Fpt.NONE__);
			}).match("size-of %0", a -> {
				return FunpSizeOf.of(p(a));
			}).match("sum %0 %1", (a, b) -> {
				return FunpTree.of(FunpOp.PLUS__, p(a), p(b), null);
			}).match("type %0 %1", (a, b) -> { // binds the type of something
				return FunpTypeCheck.of(p(a), null, p(b));
			}).match("type %0 = %1 ~ %2", (a, b, c) -> { // bind the types of two values
				return FunpTypeCheck.of(p(a), p(b), p(c));
			}).match("uncapture %0 ~ %1", (a, b) -> { // free a captured lambda
				return FunpLambdaFree.of(p(a), p(b));
			}).match(Atom.FALSE, () -> {
				return FunpBoolean.of(false);
			}).match(Atom.NIL, () -> { // form an empty array
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
						.<Funp>map(ch -> FunpCoerce.of(Coerce.NUMBER, Coerce.BYTE, FunpNumber.ofNumber(ch))) //
						.snoc(FunpCoerce.of(Coerce.NUMBER, Coerce.BYTE, FunpNumber.ofNumber(0))) //
						.toList());
				var fr = s.length() < 80 ? FunpRemark.of(s, fa) : fa;
				return FunpDefine.of(vn, fr, FunpReference.of(FunpVariable.of(vn)), Fdt.G_MONO);
			}).applyTree((op, l, r) -> {
				if (op == FunpOp.TUPLE_) {
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
			while ((t = Tree.decompose(n, FunpOp.DOT___)) != null)
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
							var b = operator == FunpOp.AND___ || operator == FunpOp.CONTD_;
							return b ? tree : Funp_.fail(null, "unknown operator " + operator);
						} else
							return null;
					}) //
					.map(n -> {
						Node[] m;
						if ((m = Funp_.pattern("%0 %1 := %2").match(n)) != null)
							return Pair.of(Atom.name(m[0]), Funp_.substitute("%0 => %1", m[1], m[2]));
						else if ((m = Funp_.pattern("%0 := %1").match(n)) != null
								|| (m = Funp_.pattern("%0: %1").match(n)) != null)
							return Pair.of(Atom.name(m[0]), m[1]);
						else
							return Pair.of(Atom.name(n), n);
					}) //
					.map2(Pair::fst, Pair::snd);
		}

		private Funp new_(Node a, boolean isDynamicSize, Funp factor) {
			var n = FunpDoHeapNew.of(isDynamicSize, factor);
			return checkDo(() -> {
				if (a == dontCare)
					return n;
				else if (factor != null)
					return FunpTypeCheck.of(FunpReference.of(FunpRepeat.of(null, p(a))), null, n);
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
				return bind(a, b, c, Atom.of("error"));
			}

			private Funp bind(Node a, Node b, Node c, Node d) {
				var vnsMutable = Mutable.of(PerSet.<String>empty());

				Iterate<Funp> iter = be -> inspect.rewrite(be, Funp.class,
						n_ -> n_.castMap(FunpVariableNew.class, f -> f.apply(vn -> {
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
				return vns_.streamlet().<Funp>fold(f1, (f, vn) -> FunpDefine.of(vn, FunpDontCare.of(), f, fdt));
			}
		}

		private boolean isArray(Node l) {
			return TreeUtil.isList(l, FunpOp.AND___);
		}

		private boolean isVar(Node v) {
			return v != dontCare && v != Atom.NIL && v instanceof Atom;
		}

		private Parse nv(String vn) {
			return new Parse(vns.replace(vn));
		}
	}

}
