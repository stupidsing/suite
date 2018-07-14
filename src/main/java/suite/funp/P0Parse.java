package suite.funp;

import static suite.util.Friends.fail;
import static suite.util.Friends.rethrow;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import suite.funp.P0.FunpDeTag;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefine.Fdt;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpDoAsm;
import suite.funp.P0.FunpDoAssignRef;
import suite.funp.P0.FunpDoAssignVar;
import suite.funp.P0.FunpDoEvalIo;
import suite.funp.P0.FunpDoWhile;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpError;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpIo;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPredefine;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpRepeat;
import suite.funp.P0.FunpSizeOf;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpTag;
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
import suite.streamlet.Streamlet;
import suite.util.ReadStream;
import suite.util.Rethrow.SourceEx;
import suite.util.Switch;
import suite.util.To;
import suite.util.Util;

public class P0Parse {

	private Atom dontCare = Atom.of("_");
	private Inspect inspect = Singleton.me.inspect;
	private String doToken = "$do";

	private int tagId;
	private Map<String, Integer> idByTag = new HashMap<>();

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
			}).match(".0 [.1]", (a, b) -> {
				return !isList(b) ? FunpIndex.of(FunpReference.of(p(a)), p(b)) : null;
			}).match(".0 => .1", (a, b) -> {
				return lambda0(a, b);
			}).match(".0, .1", (a, b) -> {
				return FunpStruct.of(List.of(Pair.of("t0", p(a)), Pair.of("t1", p(b))));
			}).match(".0/.1", (a, b) -> {
				return b instanceof Atom ? FunpField.of(FunpReference.of(p(a)), Atom.name(b)) : null;
			}).match(".0:.1", (a, b) -> {
				var tag = Atom.name(a);
				return FunpTag.of(IntMutable.of(idByTag.computeIfAbsent(tag, t -> ++tagId)), tag, p(b));
			}).match("[.0]", a -> {
				return isList(a) ? FunpArray.of(Tree.iter(a).map(this::p).toList()) : null;
			}).match("^.0", a -> {
				return FunpDeref.of(p(a));
			}).match("{ .0 }", a -> {
				return FunpStruct.of(kvs(a) //
						.map(kv -> Pair.of(kv.t0, p(kv.t1))) //
						.toList());
			}).match("address .0", a -> {
				return FunpReference.of(p(a));
			}).match("array .0 * .1", (a, b) -> {
				return FunpRepeat.of(b != Atom.of("_") ? Int.num(b) : null, p(a));
			}).match("asm .0 {.1}", (a, b) -> {
				return isDo() ? FunpDoAsm.of(Tree.iter(a, TermOp.OR____).map(n -> {
					var ma = Suite.pattern(".0 = .1").match(n);
					return Pair.of(Amd64.me.regByName.get(ma[0]), p(ma[1]));
				}).toList(), Tree.iter(b, TermOp.OR____).toList()) : fail();
			}).match("assign .0 := .1 ~ .2", (a, b, c) -> {
				var v = FunpVariable.of(Atom.name(a));
				return isDo() ? FunpDoAssignVar.of(v, p(b), p(c)) : fail();
			}).match("assign ^.0 := .1 ~ .2", (a, b, c) -> {
				return isDo() ? FunpDoAssignRef.of(FunpReference.of(p(a)), p(b), p(c)) : fail();
			}).match("byte .0", a -> {
				return FunpCoerce.of(Coerce.NUMBER, Coerce.BYTE, FunpNumber.ofNumber(num(a)));
			}).match("byte", () -> {
				return FunpCoerce.of(Coerce.NUMBER, Coerce.BYTE, FunpDontCare.of());
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
			}).match("coerce.byte .0", a -> {
				return FunpCoerce.of(Coerce.NUMBER, Coerce.BYTE, p(a));
			}).match("coerce.number .0", a -> {
				return FunpCoerce.of(Coerce.BYTE, Coerce.NUMBER, p(a));
			}).match("coerce.pointer .0", a -> {
				return FunpCoerce.of(Coerce.NUMBER, Coerce.POINTER, p(a));
			}).match("consult .0", a -> {
				return consult(Str.str(a));
			}).match("define .0 .1 := .2 ~ .3", (a, b, c, d) -> {
				return define(Fdt.L_POLY, a, lambda0(b, c), d);
			}).match("define .0 := .1 ~ .2", (a, b, c) -> {
				var lambda = lambda(a, c);
				return FunpDefine.of(Fdt.L_POLY, lambda.var, p(b), lambda.expr);
				// return parse(Suite.subst("poly .1 | (.0 => .2)", m));
			}).match("define { .0 } ~ .1", (a, b) -> {
				var list = kvs(a).collect();
				var variables1 = list.fold(variables, (vs, pair) -> vs.add(pair.t0));
				var p1 = new Parse(variables1);
				return FunpDefineRec.of(list //
						.map(pair -> Pair.of(pair.t0, p1.p(pair.t1))) //
						.toList(), p1.p(b));
			}).match("error", () -> {
				return FunpError.of();
			}).match("eval.io .0", a -> {
				return isDo() ? FunpDoEvalIo.of(p(a)) : fail();
			}).match("if (`.0` = .1) then .2 else .3", (a, b, c, d) -> {
				return bind(a, b, c, d);
			}).match("if .0 then .1 else .2", (a, b, c) -> {
				return FunpIf.of(p(a), p(b), p(c));
			}).match("io.do .0", a -> {
				return FunpIo.of(nv(doToken).p(a));
			}).match("io.for (.0 = .1; .2; .3)", (a, b, c, d) -> {
				var vn = Atom.name(a);
				var var = FunpVariable.of(vn);
				var p1 = nv(vn);
				var while_ = p1.p(c);
				var do_ = FunpDoAssignVar.of(var, FunpDoEvalIo.of(p1.p(d)), var);
				return FunpIo.of(FunpDefine.of(Fdt.L_MONO, vn, p(b), FunpDoWhile.of(while_, do_, p(Suite.parse("{}")))));
			}).match("let .0 := .1 ~ .2", (a, b, c) -> {
				var lambda = lambda(a, c);
				return FunpDefine.of(Fdt.L_MONO, lambda.var, p(b), lambda.expr);
				// return parse(Suite.subst(".1 | (.0 => .2)", m));
			}).match("let.global .0 := .1 ~ .2", (a, b, c) -> {
				return define(Fdt.GLOB, a, p(b), c);
			}).match("number .0", a -> {
				return FunpNumber.ofNumber(num(a));
			}).match("number", () -> {
				return FunpNumber.of(IntMutable.nil());
			}).match("perform.io .0 ~ .1", (a, b) -> {
				var lambda = lambda(dontCare, b);
				return isDo() ? FunpDefine.of(Fdt.L_IOAP, lambda.var, FunpDoEvalIo.of(p(a)), lambda.expr) : fail();
			}).match("predef .0", a -> {
				return FunpPredefine.of(p(a));
			}).match("size.of .0", a -> {
				return FunpSizeOf.of(p(a));
			}).match("type .0 = .1 ~ .2", (a, b, c) -> {
				return FunpCheckType.of(p(a), p(b), p(c));
			}).match(Atom.FALSE, () -> {
				return FunpBoolean.of(false);
			}).match(Atom.TRUE, () -> {
				return FunpBoolean.of(true);
			}).match(dontCare, () -> {
				return FunpDontCare.of();
			}).match("virtual .0 := .1 ~ .2", (a, b, c) -> {
				var value = p(b);
				return FunpDefine.of(Fdt.VIRT, Atom.name(a), value, p(c));
			}).applyIf(Atom.class, atom -> {
				var vn = atom.name;
				return variables.contains(vn) ? FunpVariable.of(vn) : FunpVariableNew.of(vn);
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
				return FunpDefine.of(Fdt.GLOB, vn, fa, FunpVariable.of(vn));
			}).applyTree((op, l, r) -> {
				if (op == TermOp.CONTD_)
					System.out.println(node);
				return FunpTree.of(op, p(l), p(r));
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

		private boolean isDo() {
			return variables.contains(doToken);
		}

		private Streamlet<Pair<String, Node>> kvs(Node node) {
			return Tree.iter(node, Tree::decompose).map(n -> {
				Node[] m;
				if ((m = Suite.pattern(".0 .1 := .2").match(n)) != null)
					return Pair.of(Atom.name(m[0]), Suite.substitute(".0 => .1", m[1], m[2]));
				else if ((m = Suite.pattern(".0 := .1").match(n)) != null || (m = Suite.pattern(".0: .1").match(n)) != null)
					return Pair.of(Atom.name(m[0]), m[1]);
				else
					return Pair.of(Atom.name(n), n);
			});
		}

		private FunpLambda lambda0(Node a, Node b) {
			return lambda(a, b, false);
		}

		private FunpLambda lambda(Node a, Node b) {
			return lambda(a, b, true);
		}

		private FunpLambda lambda(Node a, Node b, boolean isAllowDo) {
			var isVar = isVar(a);
			var vn = isVar ? Atom.name(a) : "l$" + Util.temp();
			var nv = isAllowDo ? nv(vn) : new Parse(variables.replace(vn).remove(doToken));
			var f = isVar ? nv.p(b) : nv.bind(a, Atom.of(vn), b);
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
			return vars.streamlet().<Funp> fold(f1, (f, var) -> FunpDefine.of(Fdt.L_MONO, var, FunpDontCare.of(), f));
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
				})).applyIf(FunpTag.class, f -> f.apply((id, tag, value_) -> {
					return new Switch<Funp>(value //
					).applyIf(FunpTag.class, g -> g.apply((id1, tag1, value1) -> {
						if (id.get() == id1.get())
							return bind(value_, value1, then, else_);
						else
							return else_;
					})).applyIf(Funp.class, g -> {
						var vn = "detag$" + Util.temp();

						// FIXME double else
						return FunpDeTag.of(id, tag, vn, value, bind(FunpVariable.of(vn), value_, then, else_), else_);
					}).result();
				})).applyIf(FunpVariable.class, f -> f.apply(var -> {
					return variables.contains(var) ? FunpDoAssignVar.of(f, value, then) : be;
				})).result();

				return result != null ? result : FunpIf.of(FunpTree.of(TermOp.EQUAL_, be, value), then, else_);
			}
		}
	}

}
