package suite.funp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import suite.Constants;
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
import suite.funp.P0.FunpDefineGlobal;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpError;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpIo;
import suite.funp.P0.FunpIoAsm;
import suite.funp.P0.FunpIoAssignReference;
import suite.funp.P0.FunpIoCat;
import suite.funp.P0.FunpIoFold;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPredefine;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpRepeat;
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
import suite.primitive.IntPrimitives.IntObj_Obj;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.Ints_;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow.SourceEx;
import suite.util.Switch;
import suite.util.To;
import suite.util.Util;

public class P0Parse {

	private Atom dontCare = Atom.of("_");
	private Inspect inspect = Singleton.me.inspect;

	public Funp parse(Node node0) {
		var node1 = expandMacros(node0);
		return new Parse(ISet.empty()).p(node1);
	}

	private Node expandMacros(Node node0) {
		class Expand {
			private IMap<Prototype, Node[]> macros;

			private Expand(IMap<Prototype, Node[]> macros) {
				this.macros = macros;
			}

			private Node expand(Node node) {
				Tree tree;
				Node[] m;
				Node[] ht;

				if ((m = Suite.pattern("expand .0 := .1 >> .2").match(node)) != null) {
					var head = m[0];
					return new Expand(macros.put(Prototype.of(head), new Node[] { head, m[1], })).expand(m[2]);
				} else if ((ht = macros.get(Prototype.of(node))) != null) {
					var g = new Generalizer();
					var t0_ = g.generalize(ht[0]);
					var t1_ = g.generalize(ht[1]);
					if (Binder.bind(node, t0_, new Trail()))
						return expand(t1_);
				}

				if ((tree = Tree.decompose(node)) != null)
					return Tree.of(tree.getOperator(), expand(tree.getLeft()), expand(tree.getRight()));
				else
					return node;
			}
		}

		return new Expand(IMap.empty()).expand(node0);
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
			}).match("[.0]", a -> {
				return FunpArray.of(Tree.iter(a, TermOp.AND___).map(this::p).toList());
			}).match(Atom.FALSE, () -> {
				return FunpBoolean.of(false);
			}).match(Atom.TRUE, () -> {
				return FunpBoolean.of(true);
			}).match("type .0 = .1 >> .2", (a, b, c) -> {
				return FunpCheckType.of(p(a), p(b), p(c));
			}).match("coerce-byte .0", a -> {
				return FunpCoerce.of(Coerce.BYTE, p(a));
			}).match("coerce-number .0", a -> {
				return FunpCoerce.of(Coerce.NUMBER, p(a));
			}).match("coerce-pointer .0", a -> {
				return FunpCoerce.of(Coerce.POINTER, p(a));
			}).match("consult .0", a -> {
				return consult(Str.str(a));
			}).match("define .0 := .1 >> .2", (a, b, c) -> {
				if (a instanceof Atom) {
					var var = Atom.name(a);
					return FunpDefine.of(true, var, p(b), parseNewVariable(c, var));
				} else
					return null;
				// return parse(Suite.subst("poly .1 | (.0 => .2)", m));
			}).match("let .0 := .1 >> .2", (a, b, c) -> {
				var var = Atom.name(a);
				if (var != null)
					return FunpDefine.of(false, var, p(b), parseNewVariable(c, var));
				// return parse(Suite.subst(".1 | (.0 => .2)", m));
				else
					return bind(a, b, c);
			}).match("define global .0 := .1 >> .2", (a, b, c) -> {
				var var = Atom.name(a);
				return FunpDefineGlobal.of(var, p(b), parseNewVariable(c, var));
			}).match("define { .0 } >> .1", (a, b) -> {
				var pattern1 = Suite.pattern(".0: .1");
				var list = Tree.iter(a, TermOp.AND___).map(pattern1::match).collect();
				var variables1 = list.fold(variables, (vs, array) -> vs.add(Atom.name(array[0])));
				var p1 = new Parse(variables1);
				return FunpDefineRec.of(list //
						.map(m -> Pair.of(Atom.name(m[0]), p1.p(m[1]))) //
						.toList(), p1.p(b));
			}).match("^.0", a -> {
				return FunpDeref.of(p(a));
			}).match(dontCare, () -> {
				return FunpDontCare.of();
			}).matchArray("error", m -> {
				return FunpError.of();
			}).match(".0/.1", (a, b) -> {
				return FunpField.of(FunpReference.of(p(a)), Atom.name(b));
			}).match("if (`.0` = .1) then .2 else .3", (a, b, c, d) -> {
				return bind(a, b, c, d);
			}).match("if .0 then .1 else .2", (a, b, c) -> {
				return FunpIf.of(p(a), p(b), p(c));
			}).match(".0:.1", (a, b) -> {
				return FunpIndex.of(FunpReference.of(p(a)), p(b));
			}).match("io .0", a -> {
				return FunpIo.of(p(a));
			}).match("io-asm .0 {.1}", (a, b) -> {
				return FunpIoAsm.of(Tree.iter(a, TermOp.OR____).map(n -> {
					var ma = Suite.pattern(".0 = .1").match(n);
					return Pair.of(Amd64.me.regByName.get(ma[0]), p(ma[1]));
				}).toList(), Tree.iter(b, TermOp.OR____).toList());
			}).match("io-cat .0", a -> {
				return FunpIoCat.of(p(a));
			}).match("io-fold .0 .1 .2", (a, b, c) -> {
				return FunpIoFold.of(p(a), p(b), p(c));
			}).match(".0 => .1", (a, b) -> {
				String var;
				Funp f;
				if (a instanceof Atom)
					f = parseNewVariable(b, var = Atom.name(a));
				else {
					var = "l$" + Util.temp();
					f = nv(var).bind(a, Atom.of(var), b);
				}
				return FunpLambda.of(var, f);
			}).applyIf(Int.class, n -> {
				return FunpNumber.ofNumber(n.number);
			}).match("predef .0", a -> {
				return FunpPredefine.of(p(a));
			}).match("address .0", a -> {
				return FunpReference.of(p(a));
			}).match(".0 * array .1", (a, b) -> {
				return FunpRepeat.of(Int.num(a), p(b));
			}).match(".0, .1", (a, b) -> {
				return FunpStruct.of(List.of(Pair.of("t0", p(a)), Pair.of("t1", p(b))));
			}).match("{ .0 }", a -> {
				return FunpStruct.of(Tree //
						.iter(a, TermOp.AND___) //
						.map(n -> {
							var m = Suite.pattern(".0: .1").match(n);
							return Pair.of(Atom.name(m[0]), p(m[1]));
						}) //
						.toList());
			}).applyTree((op, l, r) -> {
				return FunpTree.of(op, p(l), p(r));
			}).applyIf(Atom.class, atom -> {
				var var = atom.name;
				return variables.contains(var) ? FunpVariable.of(var) : FunpVariableNew.of(var);
			}).nonNullResult();
		}

		private Funp consult(String url) {
			Fun<InputStream, Funp> r0 = is -> {
				try (var isr = new InputStreamReader(is, Constants.charset)) {
					return FunpPredefine.of(parse(Suite.parse(To.string(isr))));
				} catch (IOException ex) {
					return Fail.t(ex);
				}
			};

			Fun<SourceEx<InputStream, IOException>, Funp> r1 = source -> {
				try (var is = source.source()) {
					return r0.apply(is);
				} catch (IOException ex) {
					return Fail.t(ex);
				}
			};

			if (url.startsWith("file://"))
				return r1.apply(() -> new FileInputStream(url.substring(7)));
			else if (url.startsWith("http://") || url.startsWith("https://"))
				return r0.apply(HttpUtil.get(url).inputStream());
			else
				return r1.apply(() -> getClass().getResourceAsStream(url));
		}

		private Funp bind(Node a, Node b, Node c) {
			return bind(a, b, c, Suite.parse("error"));
		}

		private Funp bind(Node a, Node b, Node c, Node d) {
			var varsMutable = Mutable.of(ISet.<String> empty());

			var be = new Object() {
				private Funp extract(Funp be) {
					return inspect.rewrite(Funp.class, n_ -> {
						return new Switch<Funp>(n_ //
						).applyIf(FunpVariableNew.class, f -> f.apply(var -> {
							varsMutable.update(varsMutable.get().replace(var));
							return FunpVariable.of(var);
						})).result();
					}, be);
				}
			}.extract(p(a));

			var vars = varsMutable.get();
			var value = p(b);
			var then = new Parse(Read.from(vars).fold(variables, ISet::add)).p(c);
			var else_ = p(d);
			var f0 = new Bind(vars).bind(be, value, then, else_);
			var f1 = FunpCheckType.of(be, value, f0);
			return Read.from(vars).<Funp> fold(f1, (f, var) -> FunpDefine.of(false, var, FunpDontCare.of(), f));
		}

		private Funp parseNewVariable(Node node, String var) {
			return nv(var).p(node);
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
				var result = be.<Funp> switch_( //
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
					return variables.contains(var) ? FunpIoAssignReference.of(FunpReference.of(f), value, then) : be;
				})).result();

				return result != null ? result : FunpIf.of(FunpTree.of(TermOp.EQUAL_, be, value), then, else_);
			}
		}
	}

}
