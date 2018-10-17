package suite.funp;

import static suite.util.Friends.fail;
import static suite.util.Friends.forInt;
import static suite.util.Friends.max;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.BindArrayUtil.Pattern;
import suite.Suite;
import suite.adt.Mutable;
import suite.adt.pair.Fixie;
import suite.adt.pair.Fixie_.Fixie3;
import suite.adt.pair.Pair;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpBoolean;
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
import suite.funp.P0.FunpMe;
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
import suite.funp.P0.FunpTypeCheck;
import suite.funp.P0.FunpVariable;
import suite.funp.P0.FunpVariableNew;
import suite.funp.P2.FunpAllocGlobal;
import suite.funp.P2.FunpAllocReg;
import suite.funp.P2.FunpAllocStack;
import suite.funp.P2.FunpAssignMem;
import suite.funp.P2.FunpAssignOp;
import suite.funp.P2.FunpCmp;
import suite.funp.P2.FunpData;
import suite.funp.P2.FunpFramePointer;
import suite.funp.P2.FunpHeapAlloc;
import suite.funp.P2.FunpHeapDealloc;
import suite.funp.P2.FunpInvoke;
import suite.funp.P2.FunpInvoke2;
import suite.funp.P2.FunpInvokeIo;
import suite.funp.P2.FunpLambdaCapture;
import suite.funp.P2.FunpMemory;
import suite.funp.P2.FunpOperand;
import suite.funp.P2.FunpRoutine;
import suite.funp.P2.FunpRoutine2;
import suite.funp.P2.FunpRoutineIo;
import suite.funp.P2.FunpSaveRegisters0;
import suite.funp.P2.FunpSaveRegisters1;
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
import suite.node.util.TreeUtil;
import suite.primitive.IntMutable;
import suite.primitive.IntPrimitives.Obj_Int;
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
	private int maxRegAlloc = 3;

	private Node typeBoolean = Atom.of("BOOLEAN");
	private Node typeByte = Atom.of("BYTE");
	private Node typeNumber = Atom.of("NUMBER");

	private Pattern typeDecorArray = Suite.pattern("ARRAY .0");
	private Pattern typeDecorIo = Suite.pattern("IO");
	private Pattern typeDecorRef = Suite.pattern("REF");

	private Pattern typePatDecor = Suite.pattern(".0: .1");
	private Pattern typePatLambda = Suite.pattern("LAMBDA .0 .1");
	private Pattern typePatStruct = Suite.pattern("STRUCT .0 .1");
	private Pattern typePatTag = Suite.pattern("TAG .0");

	private Map<Funp, Node> typeByNode = new IdentityHashMap<>();
	private Map<Funp, Boolean> isRegByNode = new IdentityHashMap<>();
	private Map<String, Var> globals = new HashMap<>();

	public Funp infer(Funp n0) {
		var t = new Reference();
		var n1 = extractPredefine(n0);
		var n2 = captureLambdas(n1);
		var checks = new ArrayList<Source<Boolean>>();

		if (unify(t, new Infer(IMap.empty(), checks, null).infer(n2))) {
			if (!Read.from(checks).isAll(Source<Boolean>::g))
				fail();

			var erase = new Erase(0, IMap.empty(), null);
			erase.erase(n2); // first pass
			return erase.erase(n2); // second pass
		} else
			return Funp_.fail(n0, "cannot infer type");
	}

	private Funp extractPredefine(Funp node0) {
		var vns = new ArrayList<String>();

		var node1 = new Object() {
			private Funp extract(Funp n) {
				return inspect.rewrite(n, Funp.class, n_ -> {
					return n_.sw( //
					).applyIf(FunpLambda.class, f -> f.apply((vn, expr, isCapture) -> {
						return FunpLambda.of(vn, extractPredefine(expr), isCapture);
					})).applyIf(FunpPredefine.class, f -> f.apply(expr -> {
						var vn = "predefine$" + Util.temp();
						vns.add(vn);
						var var = FunpVariable.of(vn);
						return FunpDoAssignVar.of(var, extract(expr), var);
					})).result();
				});
			}
		}.extract(node0);

		return Read.from(vns).fold(node1, (n, vn) -> FunpDefine.of(vn, FunpDontCare.of(), n, Fdt.L_MONO));
	}

	private Funp captureLambdas(Funp node0) {
		var grandLambda = FunpLambda.of("grand$", node0, false);
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
					return n.cast(FunpLambda.class, f -> f.apply((var, expr, isCapture) -> {
						new AssociateLambda(f).a(expr);
						return f;
					}));
				});
			}
		}

		new AssociateLambda(grandLambda).a(node0);

		class Li {
			private String capn = "cap$" + Util.temp();
			private FunpVariable cap = FunpVariable.of(capn);
			private Set<String> captureSet = new HashSet<>();
			private List<Pair<String, Funp>> captures = new ArrayList<>();
		}

		class Vi {
			private FunpLambda lambda; // variable defined here
			private boolean isRef;
			private FunpLambda varLambda; // variable read from here

			private Vi(Funp def) {
				lambda = def instanceof FunpLambda ? (FunpLambda) def : lambdaByFunp.get(def);
			}

			private FunpLambda setLambda(boolean isRef_, FunpLambda varLambda_) {
				isRef = isRef_ ? isRef_ : isRef;
				return varLambda = varLambda_;
			}
		}

		var infoByLambda = Read.from2(lambdaByFunp).values().distinct().map2(lambda -> new Li()).toMap();
		var infoByVar = Read.from2(defByVars).mapValue(Vi::new).toMap();

		new Object() {
			private Funp associate(Funp node) {
				return inspect.rewrite(node, Funp.class, n -> {
					var lambda = lambdaByFunp.get(n);

					return n.sw( //
					).doIf(FunpDoAssignVar.class, f -> {
						infoByVar.get(f.var).setLambda(true, lambda);
					}).doIf(FunpReference.class, f -> {
						f.expr.cast(FunpVariable.class, var -> infoByVar.get(var).setLambda(true, lambda));
					}).doIf(FunpVariable.class, f -> {
						infoByVar.get(f).setLambda(defByVars.get(f) instanceof FunpDefineRec, lambda);
					}).result();
				});
			}
		}.associate(node0);

		Fun2<FunpVariable, FunpLambda, Funp> accessFun = (var, lambda) -> {
			var vi = infoByVar.get(var);
			var lambdaVar = vi.lambda;
			var isRef = vi.isRef;
			var vn = var.vn;
			var access = new Object() {
				private Funp access(FunpLambda lambda_) {
					if (lambda_ == lambdaVar)
						return isRef ? FunpReference.of(var) : var;
					else if (!lambda.isCapture)
						return access(lambdaByFunp.get(lambda_));
					else {
						var li = infoByLambda.get(lambda_);
						if (li.captureSet.add(vn))
							li.captures.add(Pair.of(vn, access(lambdaByFunp.get(lambda_))));
						return FunpField.of(FunpReference.of(li.cap), vn);
					}
				}
			}.access(lambda);
			return isRef ? FunpDeref.of(access) : access;
		};

		var accessors = Read.from2(infoByVar).concatMap2((var, vi) -> {
			return vi.varLambda != null ? Read.each2(Pair.of(var, accessFun.apply(var, vi.varLambda))) : Read.empty2();
		}).toMap();

		return new Object() {
			private Funp c(Funp n) {
				return inspect.rewrite(n, Funp.class, this::c_);
			}

			private Funp c_(Funp n) {
				return n.sw( //
				).applyIf(FunpDoAssignVar.class, f -> f.apply((var, value, expr) -> {
					var accessor = accessors.get(var);
					return accessor != null ? FunpDoAssignRef.of(FunpReference.of(accessor), c(value), c(expr)) : null;
				})).applyIf(FunpLambda.class, f -> f.apply((vn, expr, isCapture) -> {
					var li = infoByLambda.get(f);
					var captures = li.captures;
					if (!captures.isEmpty()) {
						var pcapn = "pcap$" + Util.temp();
						var pcap = FunpVariable.of(pcapn);
						var struct = FunpStruct.of(captures);
						var lc = FunpLambdaCapture.of(pcap, li.cap, struct, vn, c(expr));
						var assign = FunpDoAssignRef.of(FunpReference.of(FunpDeref.of(pcap)), struct, lc);
						return FunpDefine.of(pcapn, FunpDontCare.of(), assign, Fdt.HEAP);

						// TODO free cap after use
					} else
						return null;
				})).applyIf(FunpVariable.class, f -> f.apply(vn -> {
					return accessors.get(f);
				})).result();
			}
		}.c(node0);
	}

	private class Infer {
		private IMap<String, Pair<Fdt, Node>> env;
		private List<Source<Boolean>> checks;
		private Node me;

		private Infer(IMap<String, Pair<Fdt, Node>> env, List<Source<Boolean>> checks, Node me) {
			this.env = env;
			this.checks = checks;
			this.me = me;
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
			}).applyIf(FunpCoerce.class, f -> f.apply((from, to, expr) -> {
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
			})).applyIf(FunpDefine.class, f -> f.apply((vn, value, expr, type) -> {
				var tvalue = infer(value, vn);
				return new Infer(env.replace(vn, Pair.of(type, tvalue)), checks, me).infer(expr);
			})).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr) -> {
				var pairs_ = Read.from(pairs);
				var vns = pairs_.map(Pair::fst);
				var env1 = vns.fold(env, (e, vn) -> e.put(vn, Pair.of(Fdt.L_MONO, new Reference())));
				var ts = typeStructOf(Dict.of(vns //
						.<Node, Reference> map2(Atom::of, vn -> Reference.of(env1.get(vn).t1)) //
						.toMap()), TreeUtil.buildUp(TermOp.AND___, Read.from(vns).<Node> map(Atom::of).toList()));
				var infer1 = new Infer(env1, checks, ts);

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
				infer(do_);
				return infer(expr);
			})).applyIf(FunpError.class, f -> {
				return new Reference();
			}).applyIf(FunpField.class, f -> f.apply((reference, field) -> {
				var tf = new Reference();
				var map = new HashMap<Node, Reference>();
				map.put(Atom.of(field), tf);
				unify(n, typeRefOf(typeStructOf(Dict.of(map))), infer(reference));
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
			})).applyIf(FunpLambda.class, f -> f.apply((vn, expr, isCapture) -> {
				var tv = new Reference();
				var env1 = env.replace(vn, Pair.of(Fdt.L_MONO, tv));
				return typeLambdaOf(tv, new Infer(env1, checks, me).infer(expr));
			})).applyIf(FunpLambdaCapture.class, f -> f.apply((fpIn, frameVar, frame, vn, expr) -> {
				var tv = new Reference();
				var tf = infer(frame);
				var tr = typeRefOf(tf);
				unify(n, tr, infer(fpIn));
				var env1 = IMap //
						.<String, Pair<Fdt, Node>> empty() //
						.replace(frameVar.vn, Pair.of(Fdt.L_MONO, tf)) //
						.replace(vn, Pair.of(Fdt.L_MONO, tv));
				return typeLambdaOf(tv, new Infer(env1, checks, null).infer(expr));
			})).applyIf(FunpMe.class, f -> {
				return me;
			}).applyIf(FunpNumber.class, f -> {
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
						var fs0 = Read.from(pairs).<Node> map(pair -> Atom.of(pair.t0));
						var fs1 = Read.from(types.keySet());
						for (var field : Streamlet.concat(fs0, fs1))
							if (set.add(field))
								list.add(field);
						unify(ref, TreeUtil.buildUp(TermOp.AND___, list));
					}
					return true;
				});
				return ts;
			})).applyIf(FunpTag.class, f -> f.apply((id, tag, value) -> {
				var types = new HashMap<Node, Reference>();
				types.put(Atom.of(tag), Reference.of(infer(value)));
				return typeTagOf(Dict.of(types));
			})).applyIf(FunpTagId.class, f -> f.apply(reference -> {
				unify(n, typeRefOf(typeTagOf(Dict.of())), infer(reference));
				return typeNumber;
			})).applyIf(FunpTagValue.class, f -> f.apply((reference, tag) -> {
				var tr = new Reference();
				var types = new HashMap<Node, Reference>();
				types.put(Atom.of(tag), Reference.of(tr));
				unify(n, typeRefOf(typeTagOf(Dict.of(types))), infer(reference));
				return tr;
			})).applyIf(FunpTree.class, f -> f.apply((op, lhs, rhs) -> {
				Node ti;
				if (Set.of(TermOp.BIGAND, TermOp.BIGOR_).contains(op))
					ti = typeBoolean;
				else if (Set.of(TermOp.EQUAL_, TermOp.NOTEQ_).contains(op))
					ti = new Reference();
				else
					ti = typeNumber;
				unify(n, infer(lhs), ti);
				unify(n, infer(rhs), ti);
				var cmp = Set.of(TermOp.EQUAL_, TermOp.NOTEQ_, TermOp.LE____, TermOp.LT____).contains(op);
				return cmp ? typeBoolean : ti;
			})).applyIf(FunpTree2.class, f -> f.apply((op, lhs, rhs) -> {
				unify(n, infer(lhs), typeNumber);
				unify(n, infer(rhs), typeNumber);
				return typeNumber;
			})).applyIf(FunpTypeCheck.class, f -> f.apply((left, right, expr) -> {
				unify(n, infer(left), infer(right));
				return infer(expr);
			})).applyIf(FunpVariable.class, f -> {
				return getVariable(f);
			}).applyIf(FunpVariableNew.class, f -> f.apply(vn -> {
				return Funp_.fail(f, "Undefined variable " + vn);
			})).nonNullResult();
		}

		private Node getVariable(FunpVariable var) {
			return env.get(var.vn).map((type, tv) -> type == Fdt.L_POLY ? cloneType(tv) : tv);
		}
	}

	private class Erase {
		private int scope;
		private IMap<String, Var> env;
		private Var me;

		private Erase(int scope, IMap<String, Var> env, Var me) {
			this.scope = scope;
			this.env = env;
			this.me = me;
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
			})).applyIf(FunpTypeCheck.class, f -> f.apply((left, right, expr) -> {
				return erase(expr);
			})).applyIf(FunpDefine.class, f -> f.apply((vn, value, expr, type) -> {
				if (type == Fdt.GLOB) {
					var size = getTypeSize(typeOf(value));
					var address = Mutable.<Operand> nil();
					var var = global(address, 0, size);
					var e1 = new Erase(scope, env.replace(vn, var), me);
					if (Set.of("!alloc", "!dealloc").contains(vn))
						globals.put(vn, var);
					return FunpAllocGlobal.of(size, erase(value, vn), e1.erase(expr), address);
				} else if (type == Fdt.HEAP) {
					var t = new Reference();
					unify(n, typeOf(value), typeRefOf(t));
					var size = getTypeSize(t);
					var alloc = Boolean.TRUE //
							? FunpHeapAlloc.of(size) //
							: applyOnce(FunpNumber.ofNumber(size), globals.get("!alloc").get(scope), ps);
					return defineLocal(f, vn, alloc, expr, ps);
				} else if (Set.of(Fdt.L_IOAP, Fdt.L_MONO, Fdt.L_POLY).contains(type))
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
					var var = localStack(scope, offsetStack, offset0, offset += getTypeSize(typeOf(value)));
					env1 = env1.replace(vn, var);
					assigns.add(Fixie.of(vn, var, value));
				}

				var e1 = new Erase(scope, env1, localStack(scope, offsetStack, 0, getTypeSize(type0)));
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

				var saves = Mutable.of(new ArrayList<Pair<OpReg, Integer>>());
				var fa = FunpDoAsm.of(Read.from2(assigns).mapValue(this::erase).toList(), asm);
				return FunpSaveRegisters0.of(FunpSaveRegisters1.of(fa, saves), saves);
			})).applyIf(FunpDoAssignRef.class, f -> f.apply((reference, value, expr) -> {
				return FunpAssignMem.of(memory(reference, n), erase(value), erase(expr));
			})).applyIf(FunpDoAssignVar.class, f -> f.apply((var, value, expr) -> {
				return assign(getVariable(var), erase(value), erase(expr));
			})).applyIf(FunpDoEvalIo.class, f -> f.apply(expr -> {
				return erase(expr);
			})).applyIf(FunpDoFold.class, f -> f.apply((init, cont, next) -> {
				var offset = IntMutable.nil();
				var size = getTypeSize(typeOf(init));
				var var_ = localStack(scope, offset, 0, size);
				var e1 = new Erase(scope, env.replace("fold$" + Util.temp(), var_), me);
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
			})).applyIf(FunpHeapDealloc.class, f -> f.apply((size, ref, expr) -> {
				var in = FunpData.of(List.of( //
						Pair.of(FunpNumber.ofNumber(size), IntIntPair.of(0, ps)), //
						Pair.of(ref, IntIntPair.of(ps, ps + ps))));
				applyOnce(in, globals.get("!dealloc").get(scope), ps + ps);
				return erase(expr);
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
			})).applyIf(FunpLambda.class, f -> f.apply((vn, expr, isCapture) -> {
				var b = ps + ps; // return address and EBP
				var scope1 = scope + 1;
				var lt = new LambdaType(n);
				var frame = Funp_.framePointer;
				var expr1 = new Erase(scope1, env.replace(vn, localStack(scope1, IntMutable.of(0), b, b + lt.is)), me).erase(expr);
				return eraseRoutine(lt, frame, expr1);
			})).applyIf(FunpLambdaCapture.class, f -> f.apply((fp0, frameVar, frame, vn, expr) -> {
				var b = ps + ps; // return address and EBP
				var lt = new LambdaType(n);
				var size = getTypeSize(typeOf(frame));
				var env1 = IMap //
						.<String, Var> empty() //
						.replace(frameVar.vn, localStack(0, IntMutable.of(0), 0, size)) //
						.replace(vn, localStack(1, IntMutable.of(0), b, b + lt.is));
				var fp = erase(fp0);
				var expr1 = new Erase(1, env1, null).erase(expr);
				var expr2 = FunpHeapDealloc.of(size, FunpFramePointer.of(), expr1);
				return eraseRoutine(lt, fp, expr2);
			})).applyIf(FunpMe.class, f -> {
				return me.get(scope);
			}).applyIf(FunpReference.class, f -> f.apply(expr -> {
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
				var pv = Pair.of(erase(expr), IntIntPair.of(is, is + size));
				return FunpData.of(List.of(pt, pv));
			})).applyIf(FunpTagId.class, f -> f.apply(reference -> {
				return FunpMemory.of(erase(reference), 0, is);
			})).applyIf(FunpTagValue.class, f -> f.apply((reference, tag) -> {
				return FunpMemory.of(erase(reference), is, is + getTypeSize(type0));
			})).applyIf(FunpTree.class, f -> f.apply((op, l, r) -> {
				var size0 = getTypeSize(typeOf(l));
				var size1 = getTypeSize(typeOf(r));
				if (Set.of(TermOp.EQUAL_, TermOp.NOTEQ_).contains(op) && (is < size0 || is < size1)) {
					var offsetStack = IntMutable.nil();
					var m0 = localStack(scope, offsetStack, 0, size0).getMemory(scope);
					var m1 = localStack(scope, offsetStack, size0, size0 + size1).getMemory(scope);
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
					? lambda_.apply((vn, expr, isCapture) -> defineLocal(lambda, vn, value, expr, size)) //
					: apply(value, lambda, size);
		}

		private Funp apply(Funp value, Funp lambda, int size) {
			var lt = new LambdaType(lambda);
			var lambda1 = erase(lambda);
			var saves = Mutable.of(new ArrayList<Pair<OpReg, Integer>>());
			var os = 0;
			Funp invoke;
			if (lt.os == is)
				invoke = FunpInvoke.of(lambda1);
			else if (lt.os == ps + ps)
				invoke = FunpInvoke2.of(lambda1);
			else
				invoke = FunpInvokeIo.of(lambda1, lt.is, os = lt.os);
			var as0 = allocStack(size, value, FunpSaveRegisters1.of(invoke, saves));
			var as1 = FunpAllocStack.of(os, FunpDontCare.of(), as0, IntMutable.nil());
			return FunpSaveRegisters0.of(as1, saves);
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
			isRegByNode.putIfAbsent(f, size == is);

			var operand = Mutable.<Operand> nil();
			var offset = IntMutable.nil();
			var var = local(f, operand, scope, offset, 0, size);
			var expr1 = new Erase(scope, env.replace(vn, var), me).erase(expr);

			var depth = new Object() {
				private int c(Funp node) {
					var depth = IntMutable.of(0);
					inspect.rewrite(node, Funp.class, n -> new Switch<Funp>(n //
					).doIf(FunpAllocReg.class, f -> {
						depth.update(1 + c(((FunpAllocReg) n).expr));
					}).applyIf(FunpLambda.class, f -> {
						return f;
					}).result());
					return depth.value();
				}
			}.c(expr1);

			var.setReg(depth < maxRegAlloc);

			// if erase is called twice,
			// pass 1: check for any reference accesses to locals, set
			// isRegByNode;
			// pass 2: put locals to registers according to isRegByNode.
			return var.isReg() ? FunpAllocReg.of(size, value, expr1, operand) : FunpAllocStack.of(size, value, expr1, offset);
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
					})).applyIf(FunpMe.class, f -> {
						return me.getAddress(scope);
					}).applyIf(FunpVariable.class, f -> f.apply(vn -> {
						return env.get(vn).getAddress(scope);
					})).applyIf(Funp.class, f -> {
						return Funp_.fail(f, "requires pre-definition");
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

	private Var global(Mutable<Operand> offsetOperand, int start, int end) {
		return new Var(FunpDontCare.of(), null, null, null, IntMutable.of(0), offsetOperand, start, end);
	}

	private Var local(Funp funp, Mutable<Operand> operand, int scope, IntMutable offset, int start, int end) {
		return new Var(funp, null, operand, scope, offset, null, start, end);
	}

	private Var localStack(int scope, IntMutable offset, int start, int end) {
		return new Var(FunpDontCare.of(), null, null, scope, offset, null, start, end);
	}

	private class Var {
		private Funp funp;
		private Funp value;
		private Mutable<Operand> operand;
		private Integer scope;
		private IntMutable offset;
		private Mutable<Operand> offsetOperand;
		private int start, end;

		private Var( //
				Funp funp, //
				Funp value, //
				Mutable<Operand> operand, //
				Integer scope, //
				IntMutable offset, //
				Mutable<Operand> offsetOperand, //
				int start, //
				int end) {
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

		private Funp getAddress(int scope0) {
			return getMemory(scope0).apply((p, s, e) -> FunpTree.of(TermOp.PLUS__, p, FunpNumber.ofNumber(s)));
		}

		private FunpMemory getMemory(int scope0) {
			setReg(false);
			if (value != null)
				return fail();
			else if (isReg())
				return fail();
			else
				return getMemory_(scope0);
		}

		private FunpMemory getMemory_(int scope0) {
			var nfp0 = scope != null //
					? forInt(scope, scope0).<Funp> fold(Funp_.framePointer, (i, n) -> FunpMemory.of(n, 0, ps)) // locals
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
		return unify(type0, type1) || Funp_.<Boolean> fail(n, "" //
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
		}).match(typePatDecor, (a, b) -> {
			return typePatDecor.subst(cloneNode(a), cloneType(b));
		}).match(typePatLambda, (a, b) -> {
			return typePatLambda.subst(cloneType(a), cloneType(b));
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
		return typePatDecor.subst(typeDecorArray.subst(size != null ? Int.of(size) : new Reference()), b);
	}

	private Node typeIoOf(Node a) {
		return typePatDecor.subst(typeDecorIo.subst(), a);
	}

	private Node typeLambdaOf(Node a, Node b) {
		return typePatLambda.subst(a, b);
	}

	private Node typeRefOf(Node a) {
		return typePatDecor.subst(typeDecorRef.subst(), a);
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
		Node[] m, d;
		if (n == typeBoolean)
			return Funp_.booleanSize;
		else if (n == typeByte)
			return 1;
		else if ((m = typePatDecor.match(n)) != null)
			if ((d = typeDecorArray.match(m[0])) != null) {
				var size = d[0];
				return size instanceof Int ? getTypeSize(m[1]) * Int.num(size) : fail();
			} else if ((d = typeDecorIo.match(m[0])) != null)
				return getTypeSize(m[1]);
			else if ((d = typeDecorRef.match(m[0])) != null)
				return ps;
			else
				return fail();
		else if ((m = typePatLambda.match(n)) != null)
			return ps + ps;
		else if (n == typeNumber)
			return is;
		else if ((struct = isCompletedStruct(n)) != null)
			return struct.values().toInt(Obj_Int.sum(this::getTypeSize));
		else if ((m = typePatTag.match(n)) != null) {
			var dict = Dict.m(m[0]);
			var size = 0;
			for (var t : Read.from2(dict).values())
				size = max(size, getTypeSize(t));
			return size;
		} else
			return Funp_.fail(null, "cannot get size of type " + toString(n));
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
