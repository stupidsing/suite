package suite.funp.p2;

import static java.lang.Math.max;
import static primal.statics.Fail.fail;
import static primal.statics.Fail.failBool;
import static suite.util.Streamlet_.forInt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import suite.BindArrayUtil.Pattern;
import suite.Suite;
import suite.adt.Mutable;
import suite.adt.pair.Fixie;
import suite.adt.pair.Fixie_.Fixie3;
import suite.adt.pair.Pair;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
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
import suite.funp.P0.FunpDoFold;
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
import suite.funp.P2.FunpOp;
import suite.funp.P2.FunpOperand;
import suite.funp.P2.FunpRoutine;
import suite.funp.P2.FunpRoutine2;
import suite.funp.P2.FunpRoutineIo;
import suite.funp.P2.FunpSaveRegisters0;
import suite.funp.P2.FunpSaveRegisters1;
import suite.inspect.Inspect;
import suite.lp.Trail;
import suite.lp.doer.BinderRecursive;
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
import suite.persistent.PerMap;
import suite.primitive.IntMutable;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.IntRange;
import suite.primitive.adt.map.ObjIntMap;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Source;
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
	private P20ExtractPredefine p2a = new P20ExtractPredefine();
	private P21CaptureLambda p2b = new P21CaptureLambda();

	private int is = Funp_.integerSize;
	private int ps = Funp_.pointerSize;
	private int maxRegAlloc = Funp_.isAmd64 ? 3 : 2;
	private String gcclazz = "$clazz";
	private Node gcclazzField = Atom.of(gcclazz);

	private Pattern typeDecorArray = Suite.pattern("ARRAY .0");
	private Pattern typeDecorIo = Suite.pattern("IO");
	private Pattern typeDecorRef = Suite.pattern("REF");

	private Pattern typePatDecor = Suite.pattern(".0: .1");
	private Pattern typePatInt = Suite.pattern("INT .0");
	private Pattern typePatLambda = Suite.pattern("LAMBDA .0 .1");
	private Pattern typePatStruct = Suite.pattern("STRUCT .0 .1 .2"); // STRUCT true? dict list
	private Pattern typePatTag = Suite.pattern("TAG .0");

	private Node typeBoolean = Atom.of("BOOLEAN");
	private Node typeNumber = typePatInt.subst(Int.of(is));
	private Node typeNumberp = typePatInt.subst(Int.of(ps));

	private Map<Funp, Node> typeByNode = new IdentityHashMap<>();
	private Map<Funp, Boolean> isRegByNode = new IdentityHashMap<>();

	private boolean isGcStruct = true;

	public Funp infer(Funp n0) {
		var t = new Reference();
		var n1 = p2a.extractPredefine(n0);
		var n2 = p2b.captureLambdas(n1);
		var checks = new ArrayList<Source<Boolean>>();

		if (unify(t, new Infer(PerMap.empty(), checks, null).infer(n2))) {
			var b = true //
					&& (Read.from(checks).isAll(Source<Boolean>::g) || failBool("fail type-checks")) //
					&& (getTypeSize(t) == is || failBool("invalid return type"));

			if (b) {
				var erase = new Erase(0, PerMap.empty(), null);
				erase.erase(n2); // first pass
				return erase.erase(n2); // second pass
			} else
				return fail();
		} else
			return Funp_.fail(n0, "cannot infer type");
	}

	private class Infer {
		private PerMap<String, Pair<Fdt, Node>> env;
		private List<Source<Boolean>> checks;
		private Node me;

		private Infer(PerMap<String, Pair<Fdt, Node>> env, List<Source<Boolean>> checks, Node me) {
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
					if (coerce == Coerce.BYTE || coerce == Coerce.NUMBER || coerce == Coerce.NUMBERP)
						return typePatInt.subst(Int.of(Funp_.getCoerceSize(coerce)));
					else if (coerce == Coerce.POINTER)
						return typeRefOf(new Reference());
					else
						return fail();
				};
				unify(n, tf.apply(from), infer(expr));
				return tf.apply(to);
			})).applyIf(FunpDefine.class, f -> f.apply((vn, value, expr, fdt) -> {
				var tvalue = infer(value, vn);
				return new Infer(env.replace(vn, Pair.of(fdt, tvalue)), checks, me).infer(expr);
			})).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr, fdt) -> {
				var pairs_ = Read.from(pairs);
				var vns = pairs_.map(Pair::fst);
				var env1 = vns.fold(env, (e, vn) -> e.put(vn, Pair.of(fdt, new Reference())));
				var map = vns //
						.<Node, Reference> map2(Atom::of, vn -> Reference.of(env1.get(vn).v)) //
						.toMap();
				var ts = typeStructOf( //
						Reference.of(Atom.TRUE), //
						Dict.of(map), //
						TreeUtil.buildUp(TermOp.AND___, Read.from(vns).<Node> map(Atom::of).toList()));
				var infer1 = new Infer(env1, checks, ts);

				for (var pair : pairs_)
					pair.map((vn, v) -> unify(n, env1.get(vn).v, infer1.infer(v, vn)));

				return infer1.infer(expr);
			})).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
				var t = new Reference();
				unify(n, typeRefOf(t), infer(pointer));
				return t;
			})).applyIf(FunpDoAsm.class, f -> f.apply((assigns, asm, opResult) -> {
				BiPredicate<Operand, Node> opType = (op, tp) -> {
					var size = op.size;
					if (!(tp.finalNode() instanceof Reference))
						return getTypeSize(tp) == size || Funp_.<Boolean> fail(n, null);
					else if (size == 1 || size == is || size == ps)
						return unify(n, typePatInt.subst(Int.of(size)), tp);
					else if (size == ps)
						return unify(n, typePatDecor.subst(typeDecorRef.subst(), new Reference()), tp);
					else
						return fail();
				};

				for (var assign : assigns)
					opType.test(assign.k, infer(assign.v));

				var tr = new Reference();
				opType.test(opResult, tr);
				return tr;
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
			})).applyIf(FunpDoHeapDel.class, f -> f.apply((reference, expr) -> {
				unify(n, typeRefOf(new Reference()), infer(reference));
				return infer(expr);
			})).applyIf(FunpDoHeapNew.class, f -> f.apply(() -> {
				return typeRefOf(new Reference());
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
				var env1 = PerMap //
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
				var isGcStruct_ = isGcStruct && !pairs.isEmpty();
				var pos = new ObjIntMap<String>();
				var i = 0;

				pos.put(gcclazz, i++);

				for (var pair : pairs)
					pos.put(pair.k, i++);

				var types0 = Read //
						.from2(pairs) //
						.<Node, Reference> map2((n_, v) -> Atom.of(n_), (n_, v) -> Reference.of(infer(v, n_)));

				var types1 = isGcStruct_ ? types0.cons(gcclazzField, Reference.of(typeNumberp)) : types0;
				var types2 = types1.toMap();
				var typesDict = Dict.of(types2);
				var isCompleted = new Reference();
				var ref = new Reference();
				var ts = typeStructOf(isCompleted, typesDict, ref);

				// complete the structure
				checks.add(() -> {
					unify(isCompleted, Atom.TRUE);

					if (ref.isFree()) {
						Streamlet<Node> list;

						if (isGcStruct_)
							list = Read //
									.from2(types2) //
									.sort((p0, p1) -> {
										var b0 = isReference(p0.v);
										var b1 = isReference(p1.v);
										var typeSize0 = getTypeSize(p0.v);
										var typeSize1 = getTypeSize(p1.v);
										var a0 = typeSize0 % ps == 0;
										var a1 = typeSize1 % ps == 0;
										var o0 = pos.get(Atom.name(p0.k));
										var o1 = pos.get(Atom.name(p1.k));
										var c = -Boolean.compare(b0, b1);
										c = c == 0 ? -Boolean.compare(a0, a1) : c;
										c = c == 0 ? -Integer.compare(typeSize0, typeSize1) : c;
										c = c == 0 ? Integer.compare(o0, o1) : c;
										return c;
									}) //
									.keys();
						else {
							var fs0 = Read.from(pairs).<Node> map(pair -> Atom.of(pair.k));
							var fs1 = Read.from2(types2).keys();
							list = Streamlet.concat(fs0, fs1).distinct();
						}

						unify(ref, TreeUtil.buildUp(TermOp.AND___, list.toList()));
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
			})).applyIf(FunpTypeCheck.class, f -> f.apply((lhs, rhs, expr) -> {
				Node te;
				if (rhs != null) {
					unify(n, infer(lhs), infer(rhs));
					te = infer(expr);
				} else
					unify(n, infer(lhs), te = infer(expr));
				return te;
			})).applyIf(FunpVariable.class, f -> {
				return getVariable(f);
			}).applyIf(FunpVariableNew.class, f -> f.apply(vn -> {
				return Funp_.fail(f, "Undefined variable " + vn);
			})).nonNullResult();
		}

		private Node getVariable(FunpVariable var) {
			return env.get(var.vn).map((type, tv) -> Fdt.isPoly(type) ? cloneType(tv) : tv);
		}
	}

	private class Erase {
		private int scope;
		private PerMap<String, Var> env;
		private Var me;

		private Erase(int scope, PerMap<String, Var> env, Var me) {
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
				var list = new ArrayList<Pair<Funp, IntRange>>();
				for (var element : elements) {
					var offset0 = offset;
					list.add(Pair.of(erase(element), IntRange.of(offset0, offset += elementSize)));
				}
				return FunpData.of(list);
			})).applyIf(FunpTypeCheck.class, f -> f.apply((left, right, expr) -> {
				return erase(expr);
			})).applyIf(FunpDefine.class, f -> f.apply((vn, value, expr, fdt) -> {
				if (Fdt.isGlobal(fdt)) {
					var size = getTypeSize(typeOf(value));
					var address = Mutable.<Operand> nil();
					var var = global(address, 0, size);
					var e1 = new Erase(scope, env.replace(vn, var), me);
					return FunpAllocGlobal.of(size, erase(value, vn), e1.erase(expr), address);
				} else if (fdt == Fdt.L_HEAP) {
					var t = new Reference();
					unify(n, typeOf(value), typeRefOf(t));
					var size = getTypeSize(t);
					var alloc = FunpHeapAlloc.of(size);
					return defineLocal(f, vn, alloc, expr, ps);
				} else if (Fdt.isLocal(fdt))
					return defineLocal(f, vn, value, expr);
				else if (fdt == Fdt.VIRT)
					return erase(expr);
				else
					return fail();
			})).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr, fdt) -> {
				var assigns = new ArrayList<Fixie3<String, Var, Funp>>();
				var env1 = env;
				var offset = 0;

				Fun<Erase, Funp> addAssigns = e1 -> {
					var expr1 = e1.erase(expr);

					return Read //
							.from(assigns) //
							.fold(expr1, (e, x) -> x.map((vn, v, n_) -> assign(v.get(scope), e1.erase(n_, vn), e)));
				};

				if (Fdt.isGlobal(fdt)) {
					var address = Mutable.<Operand> nil();

					for (var pair : pairs) {
						var vn = pair.k;
						var value = pair.v;
						var offset0 = offset;
						var var = global(address, offset0, offset += getTypeSize(typeOf(value)));
						env1 = env1.replace(vn, var);
						assigns.add(Fixie.of(vn, var, value));
					}

					var e1 = new Erase(scope, env1, global(address, 0, getTypeSize(type0)));
					return FunpAllocGlobal.of(offset, FunpDontCare.of(), addAssigns.apply(e1), address);
				} else if (Fdt.isLocal(fdt)) {
					var offsetStack = IntMutable.nil();

					for (var pair : pairs) {
						var vn = pair.k;
						var value = pair.v;
						var offset0 = offset;
						var var = localStack(scope, offsetStack, offset0, offset += getTypeSize(typeOf(value)));
						env1 = env1.replace(vn, var);
						assigns.add(Fixie.of(vn, var, value));
					}

					var e1 = new Erase(scope, env1, localStack(scope, offsetStack, 0, getTypeSize(type0)));
					return FunpAllocStack.of(offset, FunpDontCare.of(), addAssigns.apply(e1), offsetStack);
				} else if (fdt == Fdt.VIRT)
					return erase(expr);
				else
					return fail();
			})).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
				return FunpMemory.of(erase(pointer), 0, getTypeSize(type0));
			})).applyIf(FunpDoAsm.class, f -> f.apply((assigns, asm, opResult) -> {
				env // disable register locals
						.streamlet2() //
						.values() //
						.filter(var -> var.scope != null && var.scope == scope) //
						.sink(var -> var.setReg(false));

				var saves = Mutable.of(new ArrayList<Pair<OpReg, Integer>>());
				var fa = FunpDoAsm.of(Read.from2(assigns).mapValue(this::erase).toList(), asm, opResult);
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
			})).applyIf(FunpDoHeapDel.class, f -> f.apply((reference, expr) -> {
				var t = new Reference();
				unify(n, typeRefOf(t), typeOf(reference));
				return FunpHeapDealloc.of(getTypeSize(t), erase(reference), erase(expr));
			})).applyIf(FunpDoHeapNew.class, f -> f.apply(() -> {
				return FunpHeapAlloc.of(getTypeSize(typeOf(f)));
			})).applyIf(FunpField.class, f -> {
				return getField(f);
			}).applyIf(FunpIo.class, f -> f.apply(expr -> {
				return erase(expr);
			})).applyIf(FunpIndex.class, f -> f.apply((reference, index0) -> {
				var te = new Reference();
				unify(n, typeOf(reference), typeRefOf(typeArrayOf(null, te)));
				var size = getTypeSize(te);
				var address0 = erase(reference);
				var index1 = FunpCoerce.of(Coerce.NUMBER, Coerce.POINTER, erase(index0));
				var inc = FunpOp.of(ps, TermOp.MULT__, index1, FunpNumber.ofNumber(size));
				var address1 = FunpOp.of(ps, TermOp.PLUS__, address0, inc);
				return FunpMemory.of(address1, 0, size);
			})).applyIf(FunpLambda.class, f -> f.apply((vn, expr, isCapture) -> {
				var b = ps + ps; // return address and EBP
				var scope1 = scope + 1;
				var lt = new LambdaType(n);
				var frame = Funp_.framePointer;
				var env1 = env.replace(vn, localStack(scope1, IntMutable.of(0), b, b + lt.is));
				var expr1 = new Erase(scope1, env1, me).erase(expr);
				return eraseRoutine(lt, frame, expr1);
			})).applyIf(FunpLambdaCapture.class, f -> f.apply((fp0, frameVar, frame, vn, expr) -> {

				// the capture would free itself upon first call, therefore should not be called
				// for the second time

				var b = ps + ps; // return address and EBP
				var lt = new LambdaType(n);
				var size = getTypeSize(typeOf(frame));
				var env1 = PerMap //
						.<String, Var> empty() //
						.replace(frameVar.vn, localStack(0, IntMutable.of(0), 0, size)) //
						.replace(vn, localStack(1, IntMutable.of(0), b, b + lt.is));
				var fp = erase(fp0);
				var expr1 = new Erase(1, env1, null).erase(expr);
				var expr2 = FunpHeapDealloc.of(size, FunpMemory.of(FunpFramePointer.of(), 0, ps), expr1);
				return eraseRoutine(lt, fp, expr2);
			})).applyIf(FunpMe.class, f -> {
				return me.get(scope);
			}).applyIf(FunpReference.class, f -> f.apply(expr -> {
				return getAddress(expr);
			})).applyIf(FunpRepeat.class, f -> f.apply((count, expr) -> {
				var elementSize = getTypeSize(typeOf(expr));
				var offset = 0;
				var list = new ArrayList<Pair<Funp, IntRange>>();
				for (var i = 0; i < count; i++) {
					var offset0 = offset;
					list.add(Pair.of(expr, IntRange.of(offset0, offset += elementSize)));
				}
				return FunpData.of(list);
			})).applyIf(FunpSizeOf.class, f -> f.apply(expr -> {
				return FunpNumber.ofNumber(getTypeSize(typeOf(expr)));
			})).applyIf(FunpStruct.class, f -> f.apply(pairs_ -> {
				var map = new HashMap<Node, Reference>();
				var ts = typeStructOf(Dict.of(map));
				unify(n, ts, type0);

				var values = Read.from2(pairs_).toMap();
				var list = new ArrayList<Pair<Funp, IntRange>>();
				var offset = 0;
				var pairs = isCompletedStructList(ts);
				var clazzMut = IntMutable.nil();
				var clazz = 0;

				for (var pair : pairs) {
					var field = pair.k;
					var type = pair.v;
					var offset0 = offset;
					Funp value;

					if (field != gcclazzField) {
						var name = Atom.name(field);
						value = erase(values.get(name), name);

						if (isReference(type)) {
							var shift = offset0 / ps - 1;
							if (shift < ps - 2)
								clazz |= 1 << shift;
							else
								fail();
						}
					} else
						value = FunpCoerce.of(Coerce.NUMBER, Coerce.NUMBERP, FunpNumber.of(clazzMut));

					offset += getTypeSize(type);

					if (value != null)
						list.add(Pair.of(value, IntRange.of(offset0, offset)));
				}

				clazzMut.set(clazz);

				return FunpData.of(list);
			})).applyIf(FunpTag.class, f -> f.apply((id, tag, expr) -> {
				var size = getTypeSize(typeOf(expr));
				var pt = Pair.<Funp, IntRange> of(FunpNumber.of(id), IntRange.of(0, is));
				var pv = Pair.of(erase(expr), IntRange.of(is, is + size));
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
					return size0 == size1 ? FunpOp.of(size0, op, erase(l), erase(r)) : fail();
			})).applyIf(FunpTree2.class, f -> f.apply((op, l, r) -> {
				var size0 = getTypeSize(typeOf(l));
				var size1 = getTypeSize(typeOf(r));
				return size0 == size1 ? FunpOp.of(size0, op, erase(l), erase(r)) : fail();
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
			if (lt.os == is || lt.os == ps)
				invoke = FunpInvoke.of(lambda1, lt.is, lt.os);
			else if (lt.os == ps + ps)
				invoke = FunpInvoke2.of(lambda1, lt.is, lt.os);
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
			return var.isReg() //
					? FunpAllocReg.of(size, value, expr1, operand) //
					: FunpAllocStack.of(size, value, expr1, offset);
		}

		private Funp eraseRoutine(LambdaType lt, Funp frame, Funp expr) {
			if (lt.os == is || lt.os == ps)
				return FunpRoutine.of(frame, expr, lt.is, lt.os);
			else if (lt.os == ps + ps)
				return FunpRoutine2.of(frame, expr, lt.is, lt.os);
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
					})).applyIf(FunpField.class, f -> f.apply((ref, field) -> {
						return FunpOp.of(ps, TermOp.PLUS__, erase(ref), FunpNumber.ofNumber(getFieldOffset(f).s));
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

		private FunpMemory getField(FunpField n) {
			return getFieldOffset(n).map((s, e) -> FunpMemory.of(erase(n.reference), s, e));
		}

		private IntRange getFieldOffset(FunpField n) {
			var map = new HashMap<Node, Reference>();
			var ts = typeStructOf(Dict.of(map));
			unify(n, typeOf(n.reference), typeRefOf(ts));
			var offset = 0;
			var struct = isCompletedStructList(ts);
			if (struct != null)
				for (var pair : struct) {
					var offset1 = offset + getTypeSize(pair.v);
					if (!String_.equals(Atom.name(pair.k), n.field))
						offset = offset1;
					else
						return IntRange.of(offset, offset1);
				}
			return fail();
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
			return getMemory(scope0).apply((p, s, e) -> FunpOp.of(ps, TermOp.PLUS__, p, FunpNumber.ofNumber(s)));
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
			var nfp1 = offsetOperand != null //
					? FunpOp.of(ps, TermOp.PLUS__, nfp0, FunpOperand.of(offsetOperand)) //
					: nfp0;
			return FunpMemory.of(FunpOp.of(ps, TermOp.PLUS__, nfp1, FunpNumber.of(offset)), start, end);
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

	public Node cloneType(Node type) {
		var cloned = new IdentityHashMap<Node, Reference>();
		var cloner = new Cloner();

		return new Object() {
			private Node cloneType(Node t0) {
				var tx = cloned.get(t0);

				if (tx == null) {
					cloned.put(t0, tx = new Reference());

					var tc = new SwitchNode<Node>(t0.finalNode() //
					).match(typePatDecor, (a, b) -> {
						return typePatDecor.subst(cloner.clone(a), cloneType(b));
					}).match(typePatLambda, (a, b) -> {
						return typePatLambda.subst(cloneType(a), cloneType(b));
					}).match(typePatStruct, (a, b, c) -> {
						// clone the dict but not the completion flag or the member list
						return typePatStruct.subst(a, cloneDict(b), c);
					}).match(typePatTag, a -> {
						return typePatTag.subst(cloneDict(a));
					}).applyIf(Node.class, t -> {
						return cloner.clone(t);
					}).nonNullResult();

					if (!unify(tx, tc))
						fail();
				}

				return tx;
			}

			private Dict cloneDict(Node b) {
				var map0 = Dict.m(b);
				var map1 = Read.from2(map0).mapValue(t -> Reference.of(cloneType(t))).toMap();
				return Dict.of(map1);
			}
		}.cloneType(type);
	}

	private boolean unify(Node type0, Node type1) {
		return new BinderRecursive(new Trail()).bind(type0, type1);
		// return Binder.bind(type0, type1, new Trail());
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
		return typeStructOf(new Reference(), dict, new Reference());
	}

	private Node typeStructOf(Reference isCompleted, Dict dict, Node list) {
		return typePatStruct.subst(isCompleted, dict, list);
	}

	private Node typeTagOf(Dict dict) {
		return typePatTag.subst(dict);
	}

	private boolean isReference(Node n) {
		Node[] m;
		return (m = typePatDecor.match(n)) != null && typeDecorRef.match(m[0]) != null;
	}

	private int getTypeSize(Node n0) {
		var n = n0.finalNode();
		Collection<Reference> structMembers;
		Node[] m, d;
		if (n == typeBoolean)
			return Funp_.booleanSize;
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
		else if ((m = typePatInt.match(n)) != null)
			return Int.num(m[0]);
		else if ((m = typePatLambda.match(n)) != null)
			return ps + ps;
		else if ((structMembers = isCompletedStructSet(n)) != null)
			return Read.from(structMembers).toInt(Obj_Int.sum(this::getTypeSize));
		else if ((m = typePatTag.match(n)) != null) {
			var dict = Dict.m(m[0]);
			var size = 0;
			for (var t : Read.from2(dict).values())
				size = max(size, getTypeSize(t));
			return size;
		} else
			return Funp_.fail(null, "cannot get size of type " + toString(n));
	}

	private Collection<Reference> isCompletedStructSet(Node n) {
		var m = typePatStruct.match(n);
		return m != null && m[0] == Atom.TRUE ? Dict.m(m[1]).values() : null;
	}

	private Streamlet2<Node, Reference> isCompletedStructList(Node n) {
		var m = typePatStruct.match(n);
		if (m != null && m[0] == Atom.TRUE) {
			var dict = Dict.m(m[1]);
			return Tree.read(m[2]).map2(dict::get);
		} else
			return null;
	}

	private String toString(Node type) {
		return type.toString();
	}

}
