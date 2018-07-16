package suite.funp;

import static suite.util.Friends.fail;
import static suite.util.Friends.max;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import suite.BindArrayUtil.Pattern;
import suite.Suite;
import suite.adt.Mutable;
import suite.adt.pair.Fixie;
import suite.adt.pair.Fixie_.Fixie3;
import suite.adt.pair.Pair;
import suite.assembler.Amd64.Operand;
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
import suite.funp.P0.FunpDoAsm;
import suite.funp.P0.FunpDoAssignRef;
import suite.funp.P0.FunpDoAssignVar;
import suite.funp.P0.FunpDoEvalIo;
import suite.funp.P0.FunpDoFold;
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
import suite.funp.P0.FunpTagId;
import suite.funp.P0.FunpTagValue;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpTree2;
import suite.funp.P0.FunpVariable;
import suite.funp.P0.FunpVariableNew;
import suite.funp.P2.FunpAllocGlobal;
import suite.funp.P2.FunpAllocReg;
import suite.funp.P2.FunpAllocStack;
import suite.funp.P2.FunpAssignMem;
import suite.funp.P2.FunpAssignOp;
import suite.funp.P2.FunpCmp;
import suite.funp.P2.FunpData;
import suite.funp.P2.FunpInvoke;
import suite.funp.P2.FunpInvoke2;
import suite.funp.P2.FunpInvokeIo;
import suite.funp.P2.FunpLambdaCapture;
import suite.funp.P2.FunpMemory;
import suite.funp.P2.FunpOperand;
import suite.funp.P2.FunpRoutine;
import suite.funp.P2.FunpRoutine2;
import suite.funp.P2.FunpRoutineIo;
import suite.funp.P2.FunpSaveRegisters;
import suite.immutable.IMap;
import suite.inspect.Inspect;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Cloner;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.SwitchNode;
import suite.node.io.TermOp;
import suite.node.util.Singleton;
import suite.primitive.IntMutable;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.IntIntPair;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.Fun2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.util.String_;
import suite.util.Switch;
import suite.util.Util;

/**
 * Hindley-Milner type inference.
 *
 * @author ywsing
 */
public class P2InferType {

	private Inspect inspect = Singleton.me.inspect;

	private int is = Funp_.integerSize;
	private int ps = Funp_.pointerSize;

	private Node typeBoolean = Atom.of("BOOLEAN");
	private Node typeByte = Atom.of("BYTE");
	private Node typeNumber = Atom.of("NUMBER");
	private Pattern typePatArray = Suite.pattern("ARRAY .0 .1");
	private Pattern typePatIo = Suite.pattern("IO .0");
	private Pattern typePatLambda = Suite.pattern("LAMBDA .0 .1");
	private Pattern typePatRef = Suite.pattern("REF .0");
	private Pattern typePatStruct = Suite.pattern("STRUCT .0 .1");
	private Pattern typePatTag = Suite.pattern("TAG .0");

	private Map<Funp, Node> typeByNode = new IdentityHashMap<>();
	private Map<Funp, Boolean> isRegByNode = new IdentityHashMap<>();

	public Funp infer(Funp n0) {
		var t = new Reference();
		var n1 = extractPredefine(n0);
		var n2 = captureLambdas(n1);
		var checks = new ArrayList<Source<Boolean>>();

		if (unify(t, new Infer(IMap.empty(), checks).infer(n2))) {
			if (!Read.from(checks).isAll(Source<Boolean>::source))
				fail();

			var erase = new Erase(0, IMap.empty());
			erase.erase(n2); // first pass
			return erase.erase(n2); // second pass
		} else
			return Funp_.fail("cannot infer type for " + n0);
	}

	private Funp extractPredefine(Funp node0) {
		var vns = new ArrayList<String>();

		var node1 = new Object() {
			private Funp extract(Funp n) {
				return inspect.rewrite(n, Funp.class, n_ -> {
					return n_.sw( //
					).applyIf(FunpLambda.class, f -> f.apply((vn, expr) -> {
						return FunpLambda.of(vn, extractPredefine(expr));
					})).applyIf(FunpPredefine.class, f -> f.apply(expr -> {
						var vn = "predefine$" + Util.temp();
						var var = FunpVariable.of(vn);
						vns.add(vn);
						return FunpDoAssignVar.of(var, extract(expr), var);
					})).result();
				});
			}
		}.extract(node0);

		return Read.from(vns).fold(node1, (n, vn) -> FunpDefine.of(Fdt.L_MONO, vn, FunpDontCare.of(), n));
	}

	private Funp captureLambdas(Funp node0) {
		var grandLambda = FunpLambda.of("grand$", node0);

		var defByVars = Funp_.associateDefinitions(node0);

		var lambdaByFunp = new IdentityHashMap<Funp, FunpLambda>();

		class AssociateLambda {
			private FunpLambda lambda;

			private AssociateLambda(FunpLambda lambda) {
				this.lambda = lambda;
			}

			private Funp a(Funp node) {
				return inspect.rewrite(node, Funp.class, n -> {
					lambdaByFunp.put(n, lambda);
					return n.cast(FunpLambda.class, f -> f.apply((var, expr) -> {
						new AssociateLambda(f).a(expr);
						return f;
					}));
				});
			}
		}

		new AssociateLambda(grandLambda).a(node0);

		var lambdaByVar = Read //
				.from2(defByVars) //
				.mapValue(def -> def instanceof FunpLambda ? (FunpLambda) def : lambdaByFunp.get(def)) //
				.toMap();

		var lambdas = Read.from2(lambdaByFunp).values().distinct();
		var capnByLambda = lambdas.map2(l -> "cap$" + Util.temp()).toMap();
		var capByLambda = Read.from2(capnByLambda).mapValue(FunpVariable::of).toMap();

		var refLambdaByVar = new IdentityHashMap<FunpVariable, FunpLambda>();
		var varLambdaByVar = new IdentityHashMap<FunpVariable, FunpLambda>();

		new Object() {
			private Funp associate(Funp node) {
				return inspect.rewrite(node, Funp.class, n -> {
					Fun2<Map<FunpVariable, FunpLambda>, FunpVariable, Funp> reg = (map, var) -> {
						var lambdaVar = lambdaByVar.get(var);
						var lambda = lambdaByFunp.get(n);

						new Object() {
							private void r(FunpLambda lambda_) {
								if (lambda_ != grandLambda && lambda_ != lambdaVar) {
									map.put(var, lambda_);
									r(lambdaByFunp.get(lambda_));
								}
							}
						}.r(lambda);

						return null;
					};

					return n.sw( //
					).applyIf(FunpDoAssignVar.class, f -> f.apply((var, value, expr) -> {
						return reg.apply(refLambdaByVar, var);
					})).applyIf(FunpReference.class, f -> f.apply(expr -> {
						return expr.cast(FunpVariable.class, var -> reg.apply(refLambdaByVar, var));
					})).applyIf(FunpVariable.class, f -> {
						return reg.apply(varLambdaByVar, f);
					}).result();
				});
			}
		}.associate(node0);

		var capturesByLambda = lambdas.map2(l -> new ArrayList<Pair<String, Funp>>()).toMap();
		var isRefByVarName = new HashMap<String, Boolean>();

		refLambdaByVar.forEach((var, lambda) -> {
			var vn = var.vn;
			if (isRefByVarName.putIfAbsent(vn, true) == null)
				capturesByLambda.get(lambda).add(Pair.of(vn, FunpReference.of(var)));
		});

		varLambdaByVar.forEach((var, lambda) -> {
			var vn = var.vn;
			if (isRefByVarName.putIfAbsent(vn, false) == null)
				capturesByLambda.get(lambda).add(Pair.of(vn, var));
		});

		var accessors = Read //
				.from2(lambdaByVar) //
				.map2((var, lambdaVar) -> {
					var lambda = lambdaByFunp.get(var);
					var vn = var.vn;

					if (lambda == lambdaVar)
						return FunpVariable.of(vn);
					else {
						var access = FunpField.of(FunpReference.of(capByLambda.get(lambda)), vn);
						return isRefByVarName.get(vn) ? FunpDeref.of(access) : access;
					}
				}) //
				.toMap();

		class Capture {
			private Funp capture(Funp n) {
				return inspect.rewrite(n, Funp.class, this::capture_);
			}

			private Funp capture_(Funp n) {
				return n.sw( //
				).applyIf(FunpDoAssignVar.class, f -> f.apply((var, value, expr) -> {
					return FunpDoAssignRef.of(FunpReference.of(accessors.get(var)), capture(value), capture(expr));
				})).applyIf(FunpLambda.class, f -> f.apply((vn, expr) -> {
					var cap = capByLambda.get(f);
					if (cap != null) {
						var struct = FunpStruct.of(capturesByLambda.get(f));
						return FunpDefine.of(Fdt.GLOB, cap.vn, struct, FunpLambdaCapture.of(vn, cap, capture(expr)));
					} else
						return null;

					// TODO allocate cap on heap
					// TODO free cap after use
				})).applyIf(FunpVariable.class, f -> f.apply(vn -> {
					return accessors.get(f);
				})).result();
			}
		}

		if (Boolean.FALSE) // perform capture?
			return new Capture().capture(node0);
		else
			return node0;
	}

	private class Infer {
		private IMap<String, Pair<Fdt, Node>> env;
		private List<Source<Boolean>> checks;

		private Infer(IMap<String, Pair<Fdt, Node>> env, List<Source<Boolean>> checks) {
			this.env = env;
			this.checks = checks;
		}

		private Node infer(Funp n, String in) {
			return Funp_.rethrow(in, () -> infer(n));
		}

		private Node infer(Funp n) {
			var t = typeByNode.get(n);
			if (t == null)
				typeByNode.put(n, t = infer_(n));
			return t;
		}

		private Node infer_(Funp n) {
			return new Switch<Node>(n //
			).applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
				var tr = new Reference();
				unify(n, typeLambdaOf(infer(value), tr), infer(lambda));
				return tr;
			})).applyIf(FunpArray.class, f -> f.apply(elements -> {
				var te = new Reference();
				for (var element : elements)
					unify(n, te, infer(element));
				return typeArrayOf(elements.size(), te);
			})).applyIf(FunpBoolean.class, f -> {
				return typeBoolean;
			}).applyIf(FunpCheckType.class, f -> f.apply((left, right, expr) -> {
				unify(n, infer(left), infer(right));
				return infer(expr);
			})).applyIf(FunpCoerce.class, f -> f.apply((from, to, expr) -> {
				Fun<Coerce, Node> tf = coerce -> {
					if (coerce == Coerce.BYTE)
						return typeByte;
					else if (coerce == Coerce.NUMBER)
						return typeNumber;
					else if (coerce == Coerce.POINTER)
						return typeRefOf(new Reference());
					else
						return fail();
				};
				unify(n, tf.apply(from), infer(expr));
				return tf.apply(to);
			})).applyIf(FunpDefine.class, f -> f.apply((type, vn, value, expr) -> {
				var tvalue = infer(value, vn);
				return newEnv(env.replace(vn, Pair.of(type, tvalue))).infer(expr);
			})).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr) -> {
				var pairs_ = Read.from(pairs);
				var env1 = pairs_.fold(env, (e, pair) -> e.put(pair.t0, Pair.of(Fdt.L_MONO, new Reference())));
				var infer1 = newEnv(env1);
				for (var pair : pairs_) {
					var vn = pair.t0;
					unify(n, env1.get(vn).t1, infer1.infer(pair.t1, vn));
				}
				return infer1.infer(expr);
			})).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
				var t = new Reference();
				unify(n, typeRefOf(t), infer(pointer));
				return t;
			})).applyIf(FunpDoAsm.class, f -> f.apply((assigns, asm) -> {
				for (var assign : assigns) {
					var size = assign.t0.size;
					var tp = infer(assign.t1);
					checks.add(() -> {
						if (!(tp.finalNode() instanceof Reference))
							return getTypeSize(tp) == size;
						else if (size == Funp_.booleanSize)
							return unify(n, typeByte, tp);
						else if (size == is)
							return unify(n, typeNumber, tp);
						else
							return fail();
					});
				}
				return typeNumber;
			})).applyIf(FunpDoAssignRef.class, f -> f.apply((reference, value, expr) -> {
				unify(n, infer(reference), typeRefOf(infer(value)));
				return infer(expr);
			})).applyIf(FunpDoAssignVar.class, f -> f.apply((var, value, expr) -> {
				unify(n, getVariable(var), infer(value));
				return infer(expr);
			})).applyIf(FunpDoEvalIo.class, f -> f.apply(expr -> {
				var t = new Reference();
				unify(n, typeIoOf(t), infer(expr));
				return t;
			})).applyIf(FunpDoFold.class, f -> f.apply((init, cont, next) -> {
				var tv = new Reference();
				var tvio = typeIoOf(tv);
				unify(n, tv, infer(init));
				unify(n, typeLambdaOf(tv, typeBoolean), infer(cont));
				unify(n, typeLambdaOf(tv, tvio), infer(next));
				return tvio;
			})).applyIf(FunpDontCare.class, f -> {
				return new Reference();
			}).applyIf(FunpDoWhile.class, f -> f.apply((while_, do_, expr) -> {
				unify(n, typeBoolean, infer(while_));
				var td = infer(do_);
				if (td.finalNode() instanceof Reference)
					// enforces a type to prevent exception
					unify(n, td, typeBoolean);
				return infer(expr);
			})).applyIf(FunpError.class, f -> {
				return new Reference();
			}).applyIf(FunpField.class, f -> f.apply((reference, field) -> {
				var tf = new Reference();
				var map = new HashMap<Node, Reference>();
				map.put(Atom.of(field), tf);
				unify(n, infer(reference), typeRefOf(typeStructOf(Dict.of(map))));
				return tf;
			})).applyIf(FunpIf.class, f -> f.apply((if_, then, else_) -> {
				Node t;
				unify(n, typeBoolean, infer(if_));
				unify(n, t = infer(then), infer(else_));
				return t;
			})).applyIf(FunpIo.class, f -> f.apply(expr -> {
				return typeIoOf(infer(expr));
			})).applyIf(FunpIndex.class, f -> f.apply((reference, index) -> {
				var te = new Reference();
				unify(n, typeRefOf(typeArrayOf(null, te)), infer(reference));
				unify(n, typeNumber, infer(index));
				return te;
			})).applyIf(FunpLambda.class, f -> f.apply((vn, expr) -> {
				var tv = new Reference();
				return typeLambdaOf(tv, newEnv(env.replace(vn, Pair.of(Fdt.L_MONO, tv))).infer(expr));
			})).applyIf(FunpLambdaCapture.class, f -> f.apply((vn, cap, expr) -> {
				var tv = new Reference();
				var env0 = IMap.<String, Pair<Fdt, Node>> empty();
				var env1 = env0 //
						.replace(cap.vn, Pair.of(Fdt.L_MONO, infer(cap))) //
						.replace(vn, Pair.of(Fdt.L_MONO, tv));
				return typeLambdaOf(tv, newEnv(env1).infer(expr));
			})).applyIf(FunpNumber.class, f -> {
				return typeNumber;
			}).applyIf(FunpReference.class, f -> f.apply(expr -> {
				return typeRefOf(infer(expr));
			})).applyIf(FunpRepeat.class, f -> f.apply((count, expr) -> {
				return typeArrayOf(count, infer(expr));
			})).applyIf(FunpSizeOf.class, f -> f.apply(expr -> {
				infer(expr);
				return typeNumber;
			})).applyIf(FunpStruct.class, f -> f.apply(pairs -> {
				var types = new HashMap<Node, Reference>();
				for (var kv : pairs) {
					var name = kv.t0;
					types.put(Atom.of(name), Reference.of(infer(kv.t1, name)));
				}
				var ref = new Reference();
				var ts = typeStructOf(Dict.of(types), ref);
				checks.add(() -> {
					if (ref.isFree()) {
						var list = new ArrayList<Node>();
						var set = new HashSet<Node>();
						Streamlet<Node> fs0 = Read.from(pairs).map(pair -> Atom.of(pair.t0));
						Streamlet<Node> fs1 = Read.from(types.keySet());
						for (var field : Streamlet.concat(fs0, fs1))
							if (set.add(field))
								list.add(field);
						unify(ref, Tree.of(TermOp.AND___, list));
					}
					return true;
				});
				return ts;
			})).applyIf(FunpTag.class, f -> f.apply((id, tag, value) -> {
				var types = new HashMap<Node, Reference>();
				types.put(Atom.of(tag), Reference.of(infer(value)));
				return typeTagOf(Dict.of(types));
			})).applyIf(FunpTagId.class, f -> f.apply(expr -> {
				unify(n, typeRefOf(typeTagOf(Dict.of())), infer(expr));
				return typeNumber;
			})).applyIf(FunpTagValue.class, f -> f.apply((expr, tag) -> {
				var tr = new Reference();
				var types = new HashMap<Node, Reference>();
				types.put(Atom.of(tag), Reference.of(tr));
				unify(n, typeRefOf(typeTagOf(Dict.of(types))), infer(expr));
				return tr;
			})).applyIf(FunpTree.class, f -> f.apply((op, lhs, rhs) -> {
				Node ti;
				if (op == TermOp.BIGAND || op == TermOp.BIGOR_)
					ti = typeBoolean;
				else if (op == TermOp.EQUAL_ || op == TermOp.NOTEQ_)
					ti = new Reference();
				else
					ti = typeNumber;
				unify(n, infer(lhs), ti);
				unify(n, infer(rhs), ti);
				var cmp = op == TermOp.EQUAL_ || op == TermOp.NOTEQ_ || op == TermOp.LE____ || op == TermOp.LT____;
				return cmp ? typeBoolean : ti;
			})).applyIf(FunpTree2.class, f -> f.apply((op, lhs, rhs) -> {
				unify(n, infer(lhs), typeNumber);
				unify(n, infer(rhs), typeNumber);
				return typeNumber;
			})).applyIf(FunpVariable.class, f -> {
				return getVariable(f);
			}).applyIf(FunpVariableNew.class, f -> f.apply(vn -> {
				return Funp_.fail("Undefined variable " + vn);
			})).nonNullResult();
		}

		private Node getVariable(FunpVariable var) {
			return env.get(var.vn).map((type, tv) -> type == Fdt.L_POLY ? cloneType(tv) : tv);
		}

		private Infer newEnv(IMap<String, Pair<Fdt, Node>> env) {
			return new Infer(env, checks);
		}
	}

	private class Erase {
		private int scope;
		private IMap<String, Var> env;

		private Erase(int scope, IMap<String, Var> env) {
			this.scope = scope;
			this.env = env;
		}

		private Funp erase(Funp n, String in) {
			return Funp_.rethrow(in, () -> erase(n));
		}

		private Funp erase(Funp n) {
			return inspect.rewrite(n, Funp.class, this::erase_);
		}

		private Funp erase_(Funp n) {
			var type0 = typeOf(n);

			return n.sw( //
			).applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
				var size = getTypeSize(typeOf(value));
				return applyOnce(erase(value), lambda, size);
			})).applyIf(FunpArray.class, f -> f.apply(elements -> {
				var te = new Reference();
				unify(n, type0, typeArrayOf(null, te));
				var elementSize = getTypeSize(te);
				var offset = 0;
				var list = new ArrayList<Pair<Funp, IntIntPair>>();
				for (var element : elements) {
					var offset0 = offset;
					list.add(Pair.of(erase(element), IntIntPair.of(offset0, offset += elementSize)));
				}
				return FunpData.of(list);
			})).applyIf(FunpCheckType.class, f -> f.apply((left, right, expr) -> {
				return erase(expr);
			})).applyIf(FunpDefine.class, f -> f.apply((type, vn, value, expr) -> {
				if (type == Fdt.GLOB) {
					var size = getTypeSize(typeOf(value));
					var address = Mutable.<Operand> nil();
					var e1 = new Erase(scope, env.replace(vn, new Var(address, 0, size)));
					var m = FunpMemory.of(FunpOperand.of(address), 0, size);
					var expr1 = FunpAssignMem.of(m, erase(value, vn), e1.erase(expr));
					return FunpAllocGlobal.of(vn, size, expr1, address);
				} else if (type == Fdt.L_IOAP || type == Fdt.L_MONO || type == Fdt.L_POLY)
					return defineLocal(f, vn, value, expr);
				else if (type == Fdt.VIRT)
					return erase(expr);
				else
					return fail();
			})).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr) -> {
				var assigns = new ArrayList<Fixie3<String, Var, Funp>>();
				var offsetStack = IntMutable.nil();
				var env1 = env;
				var offset = 0;

				for (var pair : pairs) {
					var vn = pair.t0;
					var value = pair.t1;
					var offset0 = offset;
					var var = new Var(scope, offsetStack, offset0, offset += getTypeSize(typeOf(value)));
					env1 = env1.replace(vn, var);
					assigns.add(Fixie.of(vn, var, value));
				}

				var e1 = new Erase(scope, env1);
				var expr1 = e1.erase(expr);

				var expr2 = Read //
						.from(assigns) //
						.fold(expr1, (e, x) -> x.map((vn, v, n_) -> assign(v.get(scope), e1.erase(n_, vn), e)));

				return FunpAllocStack.of(offset, FunpDontCare.of(), expr2, offsetStack);
			})).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
				return FunpMemory.of(erase(pointer), 0, getTypeSize(type0));
			})).applyIf(FunpDoAsm.class, f -> f.apply((assigns, asm) -> {
				env // disable register locals
						.streamlet2() //
						.values() //
						.filter(var -> var.scope != null && var.scope == scope) //
						.sink(var -> var.setReg(false));

				return FunpSaveRegisters.of(FunpDoAsm.of(Read.from2(assigns).mapValue(this::erase).toList(), asm));
			})).applyIf(FunpDoAssignRef.class, f -> f.apply((reference, value, expr) -> {
				return FunpAssignMem.of(memory(reference, n), erase(value), erase(expr));
			})).applyIf(FunpDoAssignVar.class, f -> f.apply((var, value, expr) -> {
				return assign(getVariable(var), erase(value), erase(expr));
			})).applyIf(FunpDoEvalIo.class, f -> f.apply(expr -> {
				return erase(expr);
			})).applyIf(FunpDoFold.class, f -> f.apply((init, cont, next) -> {
				var offset = IntMutable.nil();
				var size = getTypeSize(typeOf(init));
				var var_ = new Var(scope, offset, 0, size);
				var e1 = new Erase(scope, env.replace("fold$" + Util.temp(), var_));
				var m = var_.get(scope);
				var cont_ = e1.applyOnce(m, cont, size);
				var next_ = e1.applyOnce(m, next, size);
				var while_ = FunpDoWhile.of(cont_, assign(m, next_, FunpDontCare.of()), m);
				return FunpAllocStack.of(size, e1.erase(init), while_, offset);
			})).applyIf(FunpField.class, f -> f.apply((reference, field) -> {
				var map = new HashMap<Node, Reference>();
				var ts = typeStructOf(Dict.of(map));
				unify(n, typeOf(reference), typeRefOf(ts));
				var offset = 0;
				var struct = isCompletedStruct(ts);
				if (struct != null)
					for (var pair : struct) {
						var offset1 = offset + getTypeSize(pair.t1);
						if (!String_.equals(Atom.name(pair.t0), field))
							offset = offset1;
						else
							return FunpMemory.of(erase(reference), offset, offset1);
					}
				return fail();
			})).applyIf(FunpIo.class, f -> f.apply(expr -> {
				return erase(expr);
			})).applyIf(FunpIndex.class, f -> f.apply((reference, index) -> {
				var te = new Reference();
				unify(n, typeOf(reference), typeRefOf(typeArrayOf(null, te)));
				var size = getTypeSize(te);
				var address0 = erase(reference);
				var inc = FunpTree.of(TermOp.MULT__, erase(index), FunpNumber.ofNumber(size));
				var address1 = FunpTree.of(TermOp.PLUS__, address0, inc);
				return FunpMemory.of(address1, 0, size);
			})).applyIf(FunpLambda.class, f -> f.apply((vn, expr) -> {
				var b = ps + ps; // return address and EBP
				var scope1 = scope + 1;
				var lt = new LambdaType(n);
				var frame = Funp_.framePointer;
				var expr1 = new Erase(scope1, env.replace(vn, new Var(scope1, IntMutable.of(0), b, b + lt.is))).erase(expr);
				return eraseRoutine(lt, frame, expr1);
			})).applyIf(FunpLambdaCapture.class, f -> f.apply((vn, cap, expr) -> {
				var b = ps + ps; // return address and EBP
				var lt = new LambdaType(n);
				var size = getTypeSize(typeOf(cap));
				var env0 = IMap.<String, Var> empty();
				var env1 = env0 //
						.replace(cap.vn, new Var(0, IntMutable.of(0), 0, size)) //
						.replace(vn, new Var(1, IntMutable.of(0), b, b + lt.is));
				var frame = FunpReference.of(erase(cap));
				var expr1 = new Erase(1, env1).erase(expr);
				return eraseRoutine(lt, frame, expr1);
			})).applyIf(FunpReference.class, f -> f.apply(expr -> {
				return getAddress(expr);
			})).applyIf(FunpRepeat.class, f -> f.apply((count, expr) -> {
				var elementSize = getTypeSize(typeOf(expr));
				var offset = 0;
				var list = new ArrayList<Pair<Funp, IntIntPair>>();
				for (var i = 0; i < count; i++) {
					var offset0 = offset;
					list.add(Pair.of(expr, IntIntPair.of(offset0, offset += elementSize)));
				}
				return FunpData.of(list);
			})).applyIf(FunpSizeOf.class, f -> f.apply(expr -> {
				return FunpNumber.ofNumber(getTypeSize(typeOf(expr)));
			})).applyIf(FunpStruct.class, f -> f.apply(pairs -> {
				var map = new HashMap<Node, Reference>();
				var ts = typeStructOf(Dict.of(map));
				unify(n, ts, type0);

				var values = Read.from2(pairs).toMap();
				var list = new ArrayList<Pair<Funp, IntIntPair>>();
				var offset = 0;
				var struct = isCompletedStruct(ts);

				for (var pair : struct) {
					var name = Atom.name(pair.t0);
					var type = pair.t1;
					var value = values.get(name);
					var offset0 = offset;
					offset += getTypeSize(type);
					if (value != null)
						list.add(Pair.of(erase(value, name), IntIntPair.of(offset0, offset)));
				}

				return FunpData.of(list);
			})).applyIf(FunpTag.class, f -> f.apply((id, tag, expr) -> {
				var size = getTypeSize(typeOf(expr));
				var pt = Pair.<Funp, IntIntPair> of(FunpNumber.of(id), IntIntPair.of(0, is));
				var pv = Pair.<Funp, IntIntPair> of(erase(expr), IntIntPair.of(is, is + size));
				return FunpData.of(List.of(pt, pv));
			})).applyIf(FunpTagId.class, f -> f.apply(expr -> {
				return FunpMemory.of(erase(expr), 0, is);
			})).applyIf(FunpTagValue.class, f -> f.apply((expr, tag) -> {
				return FunpMemory.of(erase(expr), is, is + getTypeSize(typeOf(f)));
			})).applyIf(FunpTree.class, f -> f.apply((op, l, r) -> {
				var size0 = getTypeSize(typeOf(l));
				var size1 = getTypeSize(typeOf(r));
				if ((op == TermOp.EQUAL_ || op == TermOp.NOTEQ_) && (is < size0 || is < size1)) {
					var offsetStack = IntMutable.nil();
					var m0 = new Var(scope, offsetStack, 0, size0).getMemory(scope);
					var m1 = new Var(scope, offsetStack, size0, size0 + size1).getMemory(scope);
					var f0 = FunpCmp.of(op, m0, m1);
					var f1 = FunpAssignMem.of(m0, erase(l), f0);
					var f2 = FunpAssignMem.of(m1, erase(r), f1);
					return FunpAllocStack.of(size0 + size1, FunpDontCare.of(), f2, offsetStack);
				} else
					return null;
			})).applyIf(FunpVariable.class, f -> f.apply(var -> {
				return getVariable(var);
			})).result();
		}

		private Funp applyOnce(Funp value, Funp lambda, int size) {
			var lambda_ = lambda.cast(FunpLambda.class);
			return lambda_ != null //
					? lambda_.apply((vn, expr) -> defineLocal(lambda, vn, value, expr, size)) //
					: apply(value, lambda, size);
		}

		private FunpSaveRegisters apply(Funp value, Funp lambda, int size) {
			var lt = new LambdaType(lambda);
			var lambda1 = erase(lambda);
			Funp invoke;
			if (lt.os == is)
				invoke = allocStack(size, value, FunpInvoke.of(lambda1));
			else if (lt.os == ps + ps)
				invoke = allocStack(size, value, FunpInvoke2.of(lambda1));
			else {
				var as = allocStack(size, value, FunpInvokeIo.of(lambda1, lt.is, lt.os));
				invoke = FunpAllocStack.of(lt.os, FunpDontCare.of(), as, IntMutable.nil());
			}
			return FunpSaveRegisters.of(invoke);
		}

		private Funp assign(Funp var, Funp value, Funp expr) {
			return var //
					.sw() //
					.applyIf(FunpMemory.class, f -> FunpAssignMem.of(f, value, expr)) //
					.applyIf(FunpOperand.class, f -> FunpAssignOp.of(f, value, expr)) //
					.nonNullResult();
		}

		private Funp defineLocal(Funp f, String vn, Funp value, Funp expr) {
			return defineLocal(f, vn, erase(value, vn), expr, getTypeSize(typeOf(value)));
		}

		private Funp defineLocal(Funp f, String vn, Funp value, Funp expr, int size) {
			var op = Mutable.<Operand> nil();
			var offset = IntMutable.nil();
			var var = new Var(f, op, scope, offset, 0, size);
			var expr1 = new Erase(scope, env.replace(vn, var)).erase(expr);

			// if erase is called twice,
			// pass 1: check for any reference accesses to locals, set
			// isRegByNode;
			// pass 2: put locals to registers according to isRegByNode.
			var n1 = var.isReg() ? FunpAllocReg.of(size, value, expr1, op) : FunpAllocStack.of(size, value, expr1, offset);

			isRegByNode.putIfAbsent(f, size == is);
			return n1;
		}

		private Funp eraseRoutine(LambdaType lt, Funp frame, Funp expr) {
			if (lt.os == is)
				return FunpRoutine.of(frame, expr);
			else if (lt.os == ps + ps)
				return FunpRoutine2.of(frame, expr);
			else
				return FunpRoutineIo.of(frame, expr, lt.is, lt.os);
		}

		private Funp getAddress(Funp expr) {
			return new Object() {
				private Funp getAddress(Funp n) {
					return n.sw( //
					).applyIf(FunpDoAssignRef.class, f -> f.apply((reference, value, expr) -> {
						return FunpAssignMem.of(memory(reference, f), erase(value), getAddress(expr));
					})).applyIf(FunpDoAssignVar.class, f -> f.apply((var, value, expr) -> {
						return assign(getVariable(var), erase(value), getAddress(expr));
					})).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
						return erase(pointer);
					})).applyIf(FunpVariable.class, f -> f.apply(vn -> {
						var m = env.get(vn).getMemory(scope);
						return m.apply((p, s, e) -> FunpTree.of(TermOp.PLUS__, p, FunpNumber.ofNumber(s)));
					})).applyIf(Funp.class, f -> {
						return Funp_.fail("requires pre-definition: " + f);
					}).nonNullResult();
				}
			}.getAddress(expr);
		}

		private Funp getVariable(FunpVariable var) {
			return getVariable(var.vn);
		}

		private Funp getVariable(String vn) {
			return env.get(vn).get(scope);
		}

		private FunpMemory memory(FunpReference reference, Funp n) {
			var t = new Reference();
			unify(n, typeOf(reference), typeRefOf(t));
			return FunpMemory.of(erase(reference), 0, getTypeSize(t));
		}

		private FunpAllocStack allocStack(int size, Funp value, Funp expr) {
			return FunpAllocStack.of(size, value, expr, IntMutable.nil());
		}
	}

	private class Var {
		private Funp funp;
		private Funp value;
		private Mutable<Operand> operand;
		private Integer scope;
		private IntMutable offset;
		private Mutable<Operand> offsetOperand;
		private int start, end;

		// global
		private Var(Mutable<Operand> offsetOperand, int start, int end) {
			this(FunpDontCare.of(), null, null, null, IntMutable.of(0), offsetOperand, start, end);
		}

		// local
		private Var(Funp funp, Mutable<Operand> operand, int scope, IntMutable offset, int start, int end) {
			this(funp, null, operand, scope, offset, null, start, end);
		}

		// local stack
		private Var(int scope, IntMutable offset, int start, int end) {
			this(FunpDontCare.of(), null, null, scope, offset, null, start, end);
		}

		private Var( //
				Funp funp, //
				Funp value, //
				Mutable<Operand> operand, //
				Integer scope, //
				IntMutable offset, //
				Mutable<Operand> offsetOperand, //
				int start, int end) {
			this.funp = funp;
			this.value = value;
			this.operand = operand;
			this.scope = scope;
			this.offset = offset;
			this.offsetOperand = offsetOperand;
			this.start = start;
			this.end = end;
		}

		private Funp get(int scope0) {
			setReg(scope != null && scope == scope0);

			if (value != null)
				return value;
			else if (isReg())
				return FunpOperand.of(operand);
			else
				return getMemory_(scope0);
		}

		private FunpMemory getMemory(int scope0) {
			setReg(false);
			return getMemory_(scope0);
		}

		private FunpMemory getMemory_(int scope0) {
			var nfp0 = scope != null //
					? Ints_.range(scope, scope0).<Funp> fold(Funp_.framePointer, (i, n) -> FunpMemory.of(n, 0, ps)) // locals
					: FunpNumber.of(IntMutable.of(0)); // globals
			var nfp1 = offsetOperand != null ? FunpTree.of(TermOp.PLUS__, nfp0, FunpOperand.of(offsetOperand)) : nfp0;
			return FunpMemory.of(FunpTree.of(TermOp.PLUS__, nfp1, FunpNumber.of(offset)), start, end);
		}

		private boolean isReg() {
			return isRegByNode.getOrDefault(funp, false);
		}

		private void setReg(boolean b) {
			if (!b)
				isRegByNode.put(funp, b);
		}
	}

	private class LambdaType {
		private int is, os;

		private LambdaType(Funp lambda) {
			var tp = new Reference();
			var tr = new Reference();
			unify(lambda, typeOf(lambda), typeLambdaOf(tp, tr));
			is = getTypeSize(tp);
			os = getTypeSize(tr);
		}
	}

	private boolean unify(Funp n, Node type0, Node type1) {
		return unify(type0, type1) || Funp_.<Boolean> fail("" //
				+ "cannot unify types between:" //
				+ "\n:: " + toString(type0) //
				+ "\n:: " + toString(type1) //
				+ "\nin " + n.getClass().getSimpleName());
	}

	private boolean unify(Node type0, Node type1) {
		return Binder.bind(type0, type1, new Trail());
	}

	public Node cloneType(Node type) {
		return new SwitchNode<Node>(type.finalNode() //
		).applyIf(Reference.class, t -> {
			return new Reference();
		}).match(typePatArray, (a, b) -> {
			return typePatArray.subst(cloneNode(a), cloneType(b));
		}).match(typePatIo, a -> {
			return typePatIo.subst(cloneType(a));
		}).match(typePatLambda, (a, b) -> {
			return typePatLambda.subst(cloneType(a), cloneType(b));
		}).match(typePatRef, a -> {
			return typePatRef.subst(cloneType(a));
		}).match(typePatStruct, (a, b) -> {
			var map0 = Dict.m(a);
			var map1 = Read.from2(map0).mapValue(t -> Reference.of(cloneType(t))).toMap();
			return typePatStruct.subst(Dict.of(map1), b);
		}).match(typePatTag, a -> {
			var map0 = Dict.m(a);
			var map1 = Read.from2(map0).mapValue(t -> Reference.of(cloneType(t))).toMap();
			return typePatTag.subst(Dict.of(map1));
		}).applyIf(Node.class, t -> {
			return t;
		}).nonNullResult();
	}

	public Node cloneNode(Node node) {
		return new Cloner().clone(node);
	}

	private Node typeOf(Funp n) {
		var type = typeByNode.get(n);
		return type != null ? type.finalNode() : fail("no type information of " + n);
	}

	private Node typeArrayOf(Integer size, Node b) {
		return typePatArray.subst(size != null ? Int.of(size) : new Reference(), b);
	}

	private Node typeIoOf(Node a) {
		return typePatIo.subst(a);
	}

	private Node typeLambdaOf(Node a, Node b) {
		return typePatLambda.subst(a, b);
	}

	private Node typeRefOf(Node a) {
		return typePatRef.subst(a);
	}

	private Node typeStructOf(Dict dict) {
		return typeStructOf(dict, new Reference());
	}

	private Node typeStructOf(Dict dict, Node list) {
		return typePatStruct.subst(dict, list);
	}

	private Node typeTagOf(Dict dict) {
		return typePatTag.subst(dict);
	}

	private int getTypeSize(Node n0) {
		var n = n0.finalNode();
		Streamlet2<Node, Reference> struct;
		Node[] m;
		if ((m = typePatArray.match(n)) != null) {
			var size = m[0];
			return size instanceof Int ? getTypeSize(m[1]) * Int.num(size) : fail();
		} else if (n == typeBoolean)
			return Funp_.booleanSize;
		else if (n == typeByte)
			return 1;
		else if ((m = typePatIo.match(n)) != null)
			return getTypeSize(m[0]);
		else if ((m = typePatLambda.match(n)) != null)
			return ps + ps;
		else if (n == typeNumber)
			return is;
		else if ((m = typePatRef.match(n)) != null)
			return ps;
		else if ((struct = isCompletedStruct(n)) != null)
			return struct.values().toInt(Obj_Int.sum(this::getTypeSize));
		else if ((m = typePatTag.match(n)) != null) {
			var dict = Dict.m(m[0]);
			var size = 0;
			for (var t : Read.from2(dict).values())
				size = max(size, getTypeSize(t));
			return size;
		} else
			return Funp_.fail("cannot get size of type " + toString(n));
	}

	private Streamlet2<Node, Reference> isCompletedStruct(Node n) {
		Node[] m = typePatStruct.match(n);
		if (m != null) {
			var dict = Dict.m(m[0]);
			return Tree.iter(m[1]).map2(dict::get);
		} else
			return null;
	}

	private String toString(Node type) {
		return type.toString();
	}

}
