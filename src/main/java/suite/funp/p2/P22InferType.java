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

import primal.MoreVerbs.Read;
import primal.Verbs.Compare;
import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.adt.Fixie;
import primal.adt.Fixie_.Fixie3;
import primal.adt.Mutable;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import primal.os.Log_;
import primal.persistent.PerMap;
import primal.persistent.PerSet;
import primal.primitive.adt.IntMutable;
import primal.primitive.adt.IntRange;
import primal.primitive.adt.map.ObjIntMap;
import primal.primitive.fp.AsInt;
import primal.streamlet.Streamlet;
import primal.streamlet.Streamlet2;
import suite.BindArrayUtil.Pattern;
import suite.Suite;
import suite.assembler.Amd64;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.funp.FunpCfg;
import suite.funp.FunpOp;
import suite.funp.Funp_;
import suite.funp.Funp_.CompileException;
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
import suite.funp.P0.FunpLambdaFree;
import suite.funp.P0.FunpLog;
import suite.funp.P0.FunpMe;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpRemark;
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
import suite.funp.P2.FunpAssignOp2;
import suite.funp.P2.FunpCmp;
import suite.funp.P2.FunpData;
import suite.funp.P2.FunpFramePointer;
import suite.funp.P2.FunpHeapAlloc;
import suite.funp.P2.FunpHeapDealloc;
import suite.funp.P2.FunpInvoke1;
import suite.funp.P2.FunpInvoke2;
import suite.funp.P2.FunpInvokeIo;
import suite.funp.P2.FunpLambdaCapture;
import suite.funp.P2.FunpMemory;
import suite.funp.P2.FunpOpLr;
import suite.funp.P2.FunpOperand;
import suite.funp.P2.FunpOperand2;
import suite.funp.P2.FunpRoutine1;
import suite.funp.P2.FunpRoutine2;
import suite.funp.P2.FunpRoutineIo;
import suite.funp.P2.FunpSaveRegisters0;
import suite.funp.P2.FunpSaveRegisters1;
import suite.funp.P2.FunpTypeAssign;
import suite.inspect.Dump;
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
import suite.node.util.Singleton;
import suite.node.util.TreeUtil;
import suite.util.Switch;

/**
 * Hindley-Milner type inference.
 *
 * @author ywsing
 */
public class P22InferType extends FunpCfg {

	private Inspect inspect = Singleton.me.inspect;

	private int is = integerSize;
	private int ps = pointerSize;
	private int maxRegAlloc = isLongMode ? 3 : 2;
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

	private boolean isGcStruct = false;

	public P22InferType(Funp_ f) {
		super(f);
	}

	public Funp infer(Funp n) {
		var t = new Reference();
		var checks0 = new ArrayList<Source<Boolean>>();
		var checks1 = new ArrayList<Source<Boolean>>();

		if (unify(t, new Infer(PerMap.empty(), checks0, checks1, null).infer(n))) {
			var b = (Read.each(checks0, checks1).concatMap(Read::from).isAll(Source::g) || failBool("fail type-checks")) //
					&& (getTypeSize(t) == is || failBool("invalid return type"));

			if (b) {
				// first pass to estimate variable usage;
				// second pass to assign registers to variables
				var erase = new Erase(0, PerMap.empty(), null);
				erase.erase(n); // first pass
				return erase.erase(n); // second pass
			} else
				return fail();
		} else
			return Funp_.fail(n, "cannot infer type");
	}

	private class Infer {
		private PerMap<String, Pair<Fdt, Node>> env;
		private List<Source<Boolean>> checks0, checks1;
		private Node me;

		private Infer(PerMap<String, Pair<Fdt, Node>> env, List<Source<Boolean>> checks0, List<Source<Boolean>> checks1, Node me) {
			this.env = env;
			this.checks0 = checks0;
			this.checks1 = checks1;
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
			).applyIf(FunpAdjustArrayPointer.class, f -> f.apply((pointer, adjust) -> {
				var tp = typeRefOf(typeArrayOf(null, new Reference()));
				unify(f, tp, infer(pointer));
				unify(f, typeNumber, infer(adjust));
				return tp;
			})).applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
				var tr = new Reference();
				unify(f, typeLambdaOf(infer(value), tr), infer(lambda));
				return tr;
			})).applyIf(FunpArray.class, f -> f.apply(elements -> {
				var te = new Reference();
				for (var element : elements)
					unify(f, te, infer(element));
				return typeArrayOf(elements.size(), te);
			})).applyIf(FunpBoolean.class, f -> {
				return typeBoolean;
			}).applyIf(FunpCoerce.class, f -> f.apply((from, to, expr) -> {
				Fun<Coerce, Node> tf = coerce -> {
					if (coerce == Coerce.BYTE || coerce == Coerce.NUMBER || coerce == Coerce.NUMBERP)
						return typePatInt.subst(Int.of(getCoerceSize(coerce)));
					else if (coerce == Coerce.POINTER)
						return typeRefOf(new Reference());
					else
						return fail();
				};
				unify(f, tf.apply(from), infer(expr));
				return tf.apply(to);
			})).applyIf(FunpDefine.class, f -> f.apply((vn, value, expr, fdt) -> {
				var tvalue = infer(value, "definition of variable '" + vn + "'");
				if (Fdt.isGlobal(fdt))
					Log_.info(vn + " :: " + toTypeString(tvalue));
				return new Infer(env.put(vn, Pair.of(fdt, tvalue)), checks0, checks1, me).infer(expr);
			})).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr, fdt) -> {
				var pairs_ = Read.from(pairs);
				var vns = pairs_.map(Pair::fst);
				var env1 = vns.fold(env, (e, vn) -> e.put(vn, Pair.of(fdt, new Reference())));
				var map = vns //
						.<Node, Reference> map2(Atom::of, vn -> Reference.of(env1.getOrFail(vn).v)) //
						.toMap();
				var ts = typeStructOf( //
						Reference.of(Atom.TRUE), //
						Dict.of(map), //
						TreeUtil.buildUp(FunpOp.AND___, Read.from(vns).<Node> map(Atom::of).toList()));
				var infer1 = new Infer(env1, checks0, checks1, ts);

				for (var pair : pairs_)
					pair.map((vn, v) -> unify( //
							f, //
							env1.getOrFail(vn).v, //
							infer1.infer(v, "definition of variable '" + vn + "'")));

				return infer1.infer(expr);
			})).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
				var t = new Reference();
				unify(f, typeRefOf(t), infer(pointer));
				return t;
			})).applyIf(FunpDoAsm.class, f -> f.apply((assigns, asm, opResult) -> {
				BiPredicate<Operand, Node> opType = (op, t) -> unify(f, typePatInt.subst(Int.of(op.size)), t);
				var tr = new Reference();
				var b = Read.from(assigns).isAll(assign -> opType.test(assign.k, infer(assign.v)));
				return b && opType.test(opResult, tr) ? tr : Funp_.fail(f, "wrong types");
			})).applyIf(FunpDoAssignRef.class, f -> f.apply((reference, value, expr) -> {
				unify(f, infer(reference), typeRefOf(infer(value)));
				return infer(expr);
			})).applyIf(FunpDoEvalIo.class, f -> f.apply(expr -> {
				var t = new Reference();
				unify(f, typeIoOf(t), infer(expr));
				return t;
			})).applyIf(FunpDoFold.class, f -> f.apply((init, cont, next) -> {
				var tv = new Reference();
				var tvio = typeIoOf(tv);
				unify(f, tv, infer(init));
				unify(f, typeLambdaOf(tv, typeBoolean), infer(cont));
				unify(f, typeLambdaOf(tv, tvio), infer(next));
				return tvio;
			})).applyIf(FunpDoHeapDel.class, f -> f.apply((isDynamicSize, reference, expr) -> {
				unify(f, typeRefOf(new Reference()), infer(reference));
				return infer(expr);
			})).applyIf(FunpDoHeapNew.class, f -> f.apply((isDynamicSize, factor) -> {
				if (factor == null)
					return typeRefOf(new Reference());
				else {
					unify(f, infer(factor), typeNumber);
					return typeRefOf(typeArrayOf(null, new Reference()));
				}
			})).applyIf(FunpDontCare.class, f -> {
				return new Reference();
			}).applyIf(FunpDoWhile.class, f -> f.apply((while_, do_, expr) -> {
				unify(f, typeBoolean, infer(while_));
				infer(do_);
				return infer(expr);
			})).applyIf(FunpError.class, f -> {
				return new Reference();
			}).applyIf(FunpField.class, f -> f.apply((reference, field) -> {
				var tf = new Reference();
				var map = new HashMap<Node, Reference>();
				map.put(Atom.of(field), tf);
				unify(f, typeRefOf(typeStructOf(Dict.of(map))), infer(reference));
				return tf;
			})).applyIf(FunpIf.class, f -> f.apply((if_, then, else_) -> {
				Node t;
				unify(f, typeBoolean, infer(if_));
				unify(f, t = infer(then), infer(else_));
				return t;
			})).applyIf(FunpIo.class, f -> f.apply(expr -> {
				return typeIoOf(infer(expr));
			})).applyIf(FunpIndex.class, f -> f.apply((reference, index) -> {
				var te = new Reference();
				unify(f, typeRefOf(typeArrayOf(null, te)), infer(reference));
				unify(f, typeNumber, infer(index));
				return te;
			})).applyIf(FunpLambda.class, f -> f.apply((vn, expr, fct) -> {
				var tv = new Reference();
				PerMap<String, Pair<Fdt, Node>> env1;
				if (fct != Fct.NOSCOP)
					env1 = env;
				else // lambda without scope can access global variables outside only
					env1 = env //
							.streamlet() //
							.filter(pair -> Fdt.isGlobal(pair.v.k) || Fdt.isSubs(pair.v.k) || pair.v.k == Fdt.VIRT) //
							.fold(PerMap.empty(), (e, p) -> e.put(p.k, p.v));
				var env2 = env1.replace(vn, Pair.of(Fdt.L_MONO, tv));
				return typeLambdaOf(tv, new Infer(env2, checks0, checks1, me).infer(expr));
			})).applyIf(FunpLambdaCapture.class, f -> f.apply((fpIn, frameVar, frame, vn, expr, fct) -> {
				var tv = new Reference();
				var tf = infer(frame);
				var tr = typeRefOf(tf);
				unify(f, tr, infer(fpIn));
				var env1 = PerMap //
						.<String, Pair<Fdt, Node>> empty() //
						.replace(frameVar.vn, Pair.of(Fdt.L_MONO, tf)) //
						.replace(vn, Pair.of(Fdt.L_MONO, tv));
				return typeLambdaOf(tv, new Infer(env1, checks0, checks1, null).infer(expr));
			})).applyIf(FunpLambdaFree.class, f -> f.apply((lambda, expr) -> {
				unify(f, infer(lambda), typeRefOf(typeLambdaOf(new Reference(), new Reference())));
				return infer(expr);
			})).applyIf(FunpLog.class, f -> f.apply((value, expr) -> {
				unify(f, infer(value), typeNumber);
				return infer(expr);
			})).applyIf(FunpMe.class, f -> {
				return me;
			}).applyIf(FunpNumber.class, f -> {
				return typeNumber;
			}).applyIf(FunpReference.class, f -> f.apply(expr -> {
				return typeRefOf(infer(expr));
			})).applyIf(FunpRemark.class, f -> f.apply((remark, expr) -> {
				return infer(expr);
			})).applyIf(FunpRepeat.class, f -> f.apply((count, expr) -> {
				return typeArrayOf(count, infer(expr));
			})).applyIf(FunpSizeOf.class, f -> f.apply(expr -> {
				infer(expr);
				return typeNumber;
			})).applyIf(FunpStruct.class, f -> f.apply((isCompleted, pairs) -> {
				var isGcStruct_ = isGcStruct && !pairs.isEmpty();
				var pos = new ObjIntMap<String>();
				var i = 0;

				pos.put(gcclazz, i++);

				for (var pair : pairs)
					pos.put(pair.k, i++);

				var types0 = Read //
						.from2(pairs) //
						.<Node, Reference> map2( //
								(n_, v) -> Atom.of(n_), //
								(n_, v) -> Reference.of(infer(v, "definition of field '" + n_ + "'")));

				var types1 = isGcStruct_ ? types0.cons(gcclazzField, Reference.of(typeNumberp)) : types0;
				var types2 = types1.toMap();
				var typesDict = Dict.of(types2);
				var isCompleted_ = isCompleted ? Reference.of(Atom.TRUE) : new Reference();
				var ref = new Reference();
				var ts = typeStructOf(isCompleted_, typesDict, ref);

				Source<Boolean> completion = () -> {
					Streamlet<Node> list;

					if (isGcStruct_)
						list = Read //
								.from2(types2) //
								.sort((p0, p1) -> {
									var isRef0 = isReference(p0.v);
									var isRef1 = isReference(p1.v);
									var typeSize0 = getTypeSize(p0.v);
									var typeSize1 = getTypeSize(p1.v);
									var isAlign0 = typeSize0 % ps == 0;
									var isAlign1 = typeSize1 % ps == 0;
									var pos0 = pos.get(Atom.name(p0.k));
									var pos1 = pos.get(Atom.name(p1.k));
									var c = -Boolean.compare(isRef0, isRef1);
									c = c == 0 ? -Boolean.compare(isAlign0, isAlign1) : c;
									c = c == 0 ? -Integer.compare(typeSize0, typeSize1) : c;
									c = c == 0 ? Integer.compare(pos0, pos1) : c;
									return c;
								}) //
								.keys();
					else {
						var fs0 = Read.from(pairs).<Node> map(pair -> Atom.of(pair.k));
						var fs1 = Read.from2(types2).keys();
						list = Streamlet.concat(fs0, fs1).distinct();
					}

					return unify(ref, TreeUtil.buildUp(FunpOp.AND___, list.toList()));
				};

				// complete the structure after all types are inferred
				checks0.add(() -> unify(isCompleted_, Atom.TRUE));
				checks1.add(() -> !ref.isFree() || completion.g());
				return ts;
			})).applyIf(FunpTag.class, f -> f.apply((id, tag, value) -> {
				var types = new HashMap<Node, Reference>();
				types.put(Atom.of(tag), Reference.of(infer(value)));
				return typeTagOf(Dict.of(types));
			})).applyIf(FunpTagId.class, f -> f.apply(reference -> {
				unify(f, typeRefOf(typeTagOf(Dict.of())), infer(reference));
				return typeNumber;
			})).applyIf(FunpTagValue.class, f -> f.apply((reference, tag) -> {
				var tr = new Reference();
				var types = new HashMap<Node, Reference>();
				types.put(Atom.of(tag), Reference.of(tr));
				unify(f, typeRefOf(typeTagOf(Dict.of(types))), infer(reference));
				return tr;
			})).applyIf(FunpTree.class, f -> f.apply((op, lhs, rhs, size) -> {
				Node ti;
				if (Set.of(FunpOp.BIGAND, FunpOp.BIGOR_).contains(op))
					ti = typeBoolean;
				else if (Set.of(FunpOp.EQUAL_, FunpOp.NOTEQ_).contains(op))
					ti = new Reference();
				else {
					var t = size != null ? Int.of(getCoerceSize(size)) : new Reference();
					ti = typePatInt.subst(t);
				}
				unify(f, infer(lhs), ti);
				unify(f, infer(rhs), ti);
				var cmp = Set.of(FunpOp.EQUAL_, FunpOp.NOTEQ_, FunpOp.LE____, FunpOp.LT____).contains(op);
				return cmp ? typeBoolean : ti;
			})).applyIf(FunpTree2.class, f -> f.apply((op, lhs, rhs) -> {
				unify(f, infer(lhs), typeNumber);
				unify(f, infer(rhs), typeNumber);
				return typeNumber;
			})).applyIf(FunpTypeAssign.class, f -> f.apply((lhs, rhs, expr) -> {
				unify(f, getVariable(lhs, false), infer(rhs));
				return infer(expr);
			})).applyIf(FunpTypeCheck.class, f -> f.apply((lhs, rhs, expr) -> {
				Node te;
				if (rhs != null) {
					unify(f, infer(lhs), infer(rhs));
					te = infer(expr);
				} else
					unify(f, infer(lhs), te = infer(expr));
				return te;
			})).applyIf(FunpVariable.class, f -> {
				return getVariable(f);
			}).applyIf(FunpVariableNew.class, f -> f.apply(vn -> {
				return Funp_.fail(f, "Undefined variable '" + vn + "'");
			})).nonNullResult();
		}

		private Node getVariable(FunpVariable var) {
			return getVariable(var, true);
		}

		private Node getVariable(FunpVariable var, boolean isCloneType) {

			// if not found, it is because the code is trying to access a outer variable
			// from a global (scope-less) closure
			return env //
					.getOpt(var.vn) //
					.map(pair -> pair.map((type, tv) -> isCloneType && Fdt.isPoly(type) ? cloneType(tv) : tv)) //
					.ifNone(() -> Funp_.fail(var, "cannot access '" + var.vn + "' due to limited scoping")) //
					.g();
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
			).applyIf(FunpAdjustArrayPointer.class, f -> f.apply((pointer, adjust) -> {
				var type = new Reference();
				unify(n, typeRefOf(typeArrayOf(null, type)), typeOf(pointer));
				return adjustPointer(pointer, adjust, getTypeSize(type));
			})).applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
				var size = getTypeSize(typeOf(value));
				return applyOnce(erase(value), lambda, size);
			})).applyIf(FunpArray.class, f -> f.apply(elements -> {
				var te = new Reference();
				unify(n, type0, typeArrayOf(null, te));
				var elementSize = elements.size() != 0 ? getTypeSize(te) : 0;
				var offset = 0;
				var list = new ArrayList<Pair<Funp, IntRange>>();
				for (var element : elements) {
					var offset0 = offset;
					list.add(Pair.of(erase(element), IntRange.of(offset0, offset += elementSize)));
				}
				return FunpData.of(list);
			})).applyIf(FunpDefine.class, f -> f.apply((vn, value, expr, fdt) -> {
				var size = getTypeSize(typeOf(value));
				if (value instanceof FunpLambda fl)
					fl.name = vn;
				if (Fdt.isGlobal(fdt)) {
					var address = Mutable.<Operand> nil();
					var var = global(address, 0, size);
					var e1 = new Erase(scope, env.replace(vn, var), me);
					var value_ = erase(value, "definition of global variable '" + vn + "'");
					return FunpAllocGlobal.of(size, value_, e1.erase(expr), address);
				} else if (Fdt.isLocal(fdt))
					return defineLocal(f, vn, erase(value, "definition of local variable '" + vn + "'"), expr, size);
				else if (Fdt.isSubs(fdt)) {
					var operands = FunpOperand2.of(Mutable.nil(), Mutable.nil());
					var var = new Var(f, operands, null, null, IntMutable.of(0), null, 0, size);
					var e1 = new Erase(scope, env.replace(vn, var), me);
					return FunpAssignOp2.of(operands, erase(value, vn), e1.erase(expr));
				} else if (fdt == Fdt.VIRT)
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
							.fold(expr1, (e, x) -> x //
									.map((vn, v, n_) -> assign( //
											v.get(scope), //
											e1.erase(n_, "definition of record variable '" + vn + "'"), //
											e)));
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
			})).applyIf(FunpDeref.class, f -> f.apply(pointer0 -> {
				var pointer1 = erase(pointer0);
				if (pointer1 instanceof FunpIndex g) {
					var elementSize = getTypeSize(type0);
					var address = adjustPointer(g.reference, g.index, elementSize);
					return FunpMemory.of(address, 0, elementSize);
				} else
					return FunpMemory.of(pointer1, 0, getTypeSize(type0));
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
				return assignRef(reference, value, erase(expr), n);
			})).applyIf(FunpDoEvalIo.class, f -> f.apply(expr -> {
				return erase(expr);
			})).applyIf(FunpDoFold.class, f -> f.apply((init, cont, next) -> {
				var offset = IntMutable.nil();
				var size = getTypeSize(typeOf(init));
				var var_ = localStack(scope, offset, 0, size);
				var e1 = new Erase(scope, env.replace("fold$" + Get.temp(), var_), me);
				var m = var_.get(scope);
				var cont_ = e1.applyOnce(m, cont, size);
				var next_ = e1.applyOnce(m, next, size);
				var while_ = FunpDoWhile.of(cont_, assign(m, next_, FunpDontCare.of()), m);
				return FunpAllocStack.of(size, e1.erase(init), while_, offset);
			})).applyIf(FunpDoHeapDel.class, f -> f.apply((isDynamicSize, reference, expr) -> {
				var t = new Reference();
				unify(n, typeRefOf(t), typeOf(reference));
				return FunpHeapDealloc.of(isDynamicSize, isDynamicSize ? -1 : getTypeSize(t), erase(reference), erase(expr));
			})).applyIf(FunpDoHeapNew.class, f -> f.apply((isDynamicSize, factor) -> {
				var t = new Reference();
				if (factor == null)
					unify(n, typeRefOf(t), typeOf(f));
				else
					unify(n, typeRefOf(typeArrayOf(null, t)), typeOf(f));
				return FunpHeapAlloc.of(isDynamicSize, getTypeSize(t), factor);
			})).applyIf(FunpField.class, f -> {
				return getField(f);
			}).applyIf(FunpIo.class, f -> f.apply(expr -> {
				return erase(expr);
			})).applyIf(FunpIndex.class, f -> f.apply((reference, index) -> {
				var te = new Reference();
				unify(n, typeOf(reference), typeRefOf(typeArrayOf(null, te)));
				var size = getTypeSize(te);
				var address1 = adjustPointer(reference, index, size);
				return FunpMemory.of(address1, 0, size);
			})).applyIf(FunpLambda.class, f -> f.apply((vn, expr0, fct) -> {
				var isScoped = fct != Fct.NOSCOP;
				var b = ps + ps; // return address and EBP
				var scope1 = isScoped ? scope + 1 : 0;
				var lt = new LambdaType(f);
				var isPassByReg = lt.isPassByReg;
				var opArg = lt.p0reg();
				var av = isPassByReg ? register(opArg, lt.is) : localStack(scope1, IntMutable.of(0), b, b + lt.is);
				var frame = isScoped ? framePointer : FunpDontCare.of();
				PerMap<String, Var> env1;

				if (isScoped)
					env1 = env;
				else // lambda without scope can access global variables outside only
					env1 = env //
							.streamlet() //
							.filter(pair -> pair.v.scope == null) //
							.fold(PerMap.empty(), (e, p) -> e.put(p.k, p.v));

				var env2 = env1.replace(vn, av);

				var expr1 = new Erase(scope1, env2, me).erase(expr0);
				var expr2 = isPassByReg ? FunpAllocReg.of(lt.is, FunpDontCare.of(), expr1, opArg) : expr1;
				var expr3 = f.name != null ? FunpRemark.of(f.name, expr2) : expr2;
				return eraseRoutine(lt, frame, expr3);
			})).applyIf(FunpLambdaCapture.class, f -> f.apply((fp0, frameVar, frame, vn, expr, fct) -> {
				var size = getTypeSize(typeOf(frame));
				var b = ps + ps; // return address and EBP
				var lt = new LambdaType(f);
				var isPassByReg = lt.isPassByReg;
				var opArg = lt.p0reg();
				var av = isPassByReg ? register(opArg, lt.is) : localStack(1, IntMutable.of(0), b, b + lt.is);

				var env1 = PerMap //
						.<String, Var> empty() //
						.replace(frameVar.vn, localStack(0, IntMutable.of(0), 0, size)) //
						.replace(vn, av);

				var fp1 = erase(fp0);
				var expr1 = new Erase(1, env1, null).erase(expr);
				var expr2 = isPassByReg ? FunpAllocReg.of(lt.is, FunpDontCare.of(), expr1, opArg) : expr1;
				Funp expr3;

				if (fct == Fct.MANUAL)
					expr3 = expr2;
				else if (fct == Fct.ONCE__)
					// the capture would free itself upon first call, therefore should not be called
					// for the second time
					expr3 = FunpHeapDealloc.of(false, size, FunpMemory.of(FunpFramePointer.of(), 0, ps), expr2);
				else
					return fail();

				return eraseRoutine(lt, fp1, expr3);
			})).applyIf(FunpLambdaFree.class, f -> f.apply((lambdaRef, expr) -> {
				return FunpHeapDealloc.of(true, 0, FunpMemory.of(erase(lambdaRef), 0, ps), erase(expr));
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
			})).applyIf(FunpStruct.class, f -> f.apply((isCompleted, pairs_) -> {
				var map = new HashMap<Node, Reference>();
				var ts = typeStructOf(Dict.of(map));
				unify(n, ts, type0);

				var values = Read.from2(pairs_).toMap();
				var list = new ArrayList<Pair<Funp, IntRange>>();
				var offset = 0;
				var pairs = isCompletedStructList(ts);
				var clazzMut = IntMutable.nil();
				var clazz = 0;

				// generate garbage-collection information for the structure:
				// a bit-array to indicate if there are reference fields.
				for (var pair : pairs) {
					var field = pair.k;
					var type = pair.v;
					var offset0 = offset;
					Funp value;

					if (field != gcclazzField) {
						var name = Atom.name(field);
						var v = values.get(name);
						if (v != null)
							value = erase(v, "definition of field '" + name + "'");
						else
							value = Funp_.fail(f, "no definition for field '" + name + "'");

						if (isReference(type)) {
							var shift = offset0 / ps - 1;
							if (shift < (ps - 2) * 8)
								clazz |= 1 << shift;
							else
								Funp_.fail(f, "too many struct members");
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
			})).applyIf(FunpTree.class, f -> f.apply((op, l, r, size) -> {
				var size0 = getTypeSize(typeOf(l));
				var size1 = getTypeSize(typeOf(r));
				if (Set.of(FunpOp.EQUAL_, FunpOp.NOTEQ_).contains(op) && (!is1248(size0) || !is1248(size1))) {
					var offsetStack0 = IntMutable.nil();
					var offsetStack1 = IntMutable.nil();
					var ml = l.cast(FunpMemory.class);
					var mr = r.cast(FunpMemory.class);
					var m0 = ml != null ? ml : localStack(scope, offsetStack0, 0, size0).getMemory(scope);
					var m1 = mr != null ? mr : localStack(scope, offsetStack1, 0, size1).getMemory(scope);
					var f0 = FunpCmp.of(op, m0, m1);
					var f1 = ml != null ? f0 : FunpAssignMem.of(m0, erase(l), f0);
					var f2 = mr != null ? f1 : FunpAssignMem.of(m1, erase(r), f1);
					var f3 = FunpAllocStack.of(size0, FunpDontCare.of(), f2, offsetStack0);
					var f4 = FunpAllocStack.of(size1, FunpDontCare.of(), f3, offsetStack1);
					return f4;
				} else
					return size0 == size1 ? FunpOpLr.of(size0, op, erase(l), erase(r)) : Funp_.fail(f, "wrong sizes");
			})).applyIf(FunpTree2.class, f -> f.apply((op, l, r) -> {
				var size0 = getTypeSize(typeOf(l));
				var size1 = getTypeSize(typeOf(r));
				return size0 == size1 ? FunpOpLr.of(size0, op, erase(l), erase(r)) : Funp_.fail(f, "wrong sizes");
			})).applyIf(FunpTypeAssign.class, f -> f.apply((lhs, rhs, expr) -> {
				return erase(expr);
			})).applyIf(FunpTypeCheck.class, f -> f.apply((lhs, rhs, expr) -> {
				return erase(expr);
			})).applyIf(FunpVariable.class, f -> f.apply(var -> {
				return getVariable(var);
			})).result();
		}

		private Funp applyOnce(Funp value, Funp lambda, int size) {
			if (lambda instanceof FunpLambda lambda_) // expands the lambda directly
				return lambda_.apply((vn, expr, fct) -> defineLocal(lambda, vn, value, expr, size));
			else
				return apply(value, lambda, size);
		}

		private Funp apply(Funp value, Funp lambda, int size) {
			var lt = new LambdaType(lambda);
			var lambda1 = erase(lambda);
			var saves = Mutable.of(new ArrayList<Pair<OpReg, Integer>>());
			var isPassByReg = lt.isPassByReg;
			var is_ = isPassByReg ? 0 : lt.is;
			int os_;
			Funp invoke;
			if (lt.os == is || lt.os == ps)
				invoke = FunpInvoke1.of(lambda1, lt.is, lt.os, is_, os_ = 0);
			else if (lt.os == ps + ps)
				invoke = FunpInvoke2.of(lambda1, lt.is, lt.os, is_, os_ = 0);
			else
				invoke = FunpInvokeIo.of(lambda1, lt.is, lt.os, is_, os_ = lt.os);
			var reg = Mutable.<Operand> nil();
			var op = size == is ? FunpOperand.of(reg) : null;
			var value_ = op != null ? op : value;
			var opArg = lt.p0reg();
			var as0 = isPassByReg ? FunpAllocReg.of(lt.is, value_, invoke, opArg) : invoke;
			var as1 = FunpSaveRegisters1.of(as0, saves);
			var as2 = isPassByReg ? as1 : allocStack(size, value_, as1);
			var as3 = allocStack(os_, FunpDontCare.of(), as2);
			var as4 = op != null ? FunpAssignOp.of(op, value, as3) : as3;
			var as5 = FunpSaveRegisters0.of(as4, saves);
			return as5;
		}

		private FunpOpLr adjustPointer(Funp address, Funp index, int size) {
			var index_ = FunpCoerce.of(Coerce.NUMBER, Coerce.NUMBERP, erase(index));
			var size_ = FunpCoerce.of(Coerce.NUMBER, Coerce.NUMBERP, FunpNumber.ofNumber(size));
			var inc = FunpOpLr.of(ps, FunpOp.MULT__, size_, index_);
			return FunpOpLr.of(ps, FunpOp.PLUS__, erase(address), inc);
		}

		private Funp getAddress(Funp expr) {
			return new Object() {
				private Funp getAddress(Funp n) {
					return n.sw( //
					).applyIf(FunpDoAssignRef.class, f -> f.apply((reference, value, expr) -> {
						return FunpAssignMem.of(memory(reference, f), erase(value), getAddress(expr));
					})).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
						return erase(pointer);
					})).applyIf(FunpField.class, f -> f.apply((ref, field) -> {
						return FunpOpLr.of(ps, FunpOp.PLUS__, erase(ref), FunpNumber.ofNumber(getFieldOffset(f).s));
					})).applyIf(FunpIndex.class, f -> f.apply((ref, index) -> {
						var type = new Reference();
						unify(f, typeRefOf(typeArrayOf(null, type)), typeOf(ref));
						return adjustPointer(ref, index, getTypeSize(type));
					})).applyIf(FunpMe.class, f -> {
						return me.getAddress(scope);
					}).applyIf(FunpVariable.class, f -> f.apply(vn -> {
						return env.getOrFail(vn).getAddress(scope);
					})).applyIf(FunpTypeAssign.class, f -> f.apply((left, right, expr) -> {
						return getAddress(expr);
					})).applyIf(FunpTypeCheck.class, f -> f.apply((left, right, expr) -> {
						return getAddress(expr);
					})).applyIf(Funp.class, f -> {
						return Funp_.fail(f, "requires pre-definition of a " + f);
					}).nonNullResult();
				}
			}.getAddress(expr);
		}

		private Funp assignRef(FunpReference reference, Funp value, Funp expr, Funp n) {
			var value_ = erase(value);
			return reference.expr instanceof FunpOperand op //
				? FunpAssignOp.of(op, value_, expr) //
				: reference.expr instanceof FunpVariable var //
				? assign(erase(var), value_, expr) //
				: FunpAssignMem.of(memory(reference, n), value_, expr);
		}

		private Funp assign(Funp var, Funp value, Funp expr) {
			return var //
					.sw() //
					.applyIf(FunpMemory.class, f -> FunpAssignMem.of(f, value, expr)) //
					.applyIf(FunpOperand.class, f -> FunpAssignOp.of(f, value, expr)) //
					.nonNullResult();
		}

		private Funp defineLocal(Funp f, String vn, Funp value, Funp expr, int size) {
			isRegByNode.putIfAbsent(f, size == is);

			var operand = Mutable.<Operand> nil();
			var offset = IntMutable.nil();
			var var = local(f, operand, scope, offset, 0, size);
			var expr1 = new Erase(scope, env.replace(vn, var), me).erase(expr);

			// the register allocation in sub-expressions would have priority.
			// if there are already many register allocation in the value expression,
			// we would not do further.
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
			var is_ = lt.isPassByReg ? 0 : lt.is;
			if (lt.os == is || lt.os == ps)
				return FunpRoutine1.of(frame, expr, lt.is, lt.os, is_, 0);
			else if (lt.os == ps + ps)
				return FunpRoutine2.of(frame, expr, lt.is, lt.os, is_, 0);
			else
				return FunpRoutineIo.of(frame, expr, lt.is, lt.os, is_, lt.os);
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
				for (var pair : struct)
					try {
						var offset1 = offset + getTypeSize(pair.v);
						if (!Equals.string(Atom.name(pair.k), n.field))
							offset = offset1;
						else
							return IntRange.of(offset, offset1);
					} catch (Exception ex) {
						throw new CompileException(n, "for field '" + pair.k + "'", ex);
					}
			return Funp_.fail(n, "field '" + n.field + "' not found");
		}

		private Funp getVariable(String vn) {
			return env.getOrFail(vn).get(scope);
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

	private Var localStack(int scope, IntMutable offset, int start, int end) {
		return local(FunpDontCare.of(), null, scope, offset, start, end);
	}

	private Var local(Funp funp, Mutable<Operand> operand, int scope, IntMutable offset, int start, int end) {
		return new Var(funp, null, operand, scope, offset, null, start, end);
	}

	private Var register(Mutable<Operand> operand, int size) {
		return new Var(FunpDontCare.of(), FunpOperand.of(operand), null, null, IntMutable.of(0), null, 0, size);
	}

	private class Var {

		// FunpDefine, FunpDefineRec or FunpLambda that defines the variable
		private Funp definition;

		private Funp value; // immutable value of the variable, if any
		private Mutable<Operand> operand; // operand storing the variable, if any

		// variable addressing definition
		private Integer scope; // level of frame scopes, or null if global
		private IntMutable offset; // offset to a label
		private Mutable<Operand> offsetOperand; // offset operand
		private int start, end;

		private Var( //
				Funp definition, //
				Funp value, //
				Mutable<Operand> operand, //
				Integer scope, //
				IntMutable offset, //
				Mutable<Operand> offsetOperand, //
				int start, //
				int end) {
			this.definition = definition;
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
			return getMemory(scope0).apply((p, s, e) -> FunpOpLr.of(ps, FunpOp.PLUS__, p, FunpNumber.ofNumber(s)));
		}

		private FunpMemory getMemory(int scope0) {
			setReg(false);
			if (value != null)
				return fail("cannot get address of " + Dump.toLine(value));
			else if (isReg())
				return fail();
			else
				return getMemory_(scope0);
		}

		private FunpMemory getMemory_(int scope0) {
			var frame = scope != null //
					? forInt(scope, scope0).<Funp> fold(framePointer, (i, n) -> FunpMemory.of(n, 0, ps)) //
					: null;

			var nfp0 = FunpNumber.of(offset);
			var nfp1 = frame != null ? FunpOpLr.of(ps, FunpOp.PLUS__, frame, nfp0) : nfp0;
			var nfp2 = offsetOperand != null ? FunpOpLr.of(ps, FunpOp.PLUS__, nfp1, FunpOperand.of(offsetOperand)) : nfp1;
			return FunpMemory.of(nfp2, start, end);
		}

		private boolean isReg() {
			return isRegByNode.getOrDefault(definition, false);
		}

		private void setReg(boolean b) {
			if (!b)
				isRegByNode.put(definition, b);
		}
	}

	private class LambdaType {
		private int is, os;
		private boolean isPassByReg;

		private LambdaType(Funp lambda) {
			var tp = new Reference();
			var tr = new Reference();
			unify(lambda, typeOf(lambda), typeLambdaOf(tp, tr));

			var tp_ = tp.finalNode();

			is = getTypeSize(tp);
			os = getTypeSize(tr);

			isPassByReg = false //
					|| typePatInt.match(tp_) != null && is == P22InferType.this.is //
					|| typePatInt.match(tp_) != null && is == P22InferType.this.ps //
					|| isReference(tp_);
		}

		private Mutable<Operand> p0reg() {
			return isPassByReg ? Mutable.of(Amd64.me.regs(is)[Amd64.me.axReg]) : null;
		}
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

					unify(FunpDontCare.of(), tx, tc);
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

	private boolean unify(Funp n, Node type0, Node type1) {
		return unify(type0, type1) || Funp_.<Boolean> fail(n, "" //
				+ "cannot unify types between:" //
				+ "\n:: " + toTypeString(type0) //
				+ "\n:: " + toTypeString(type1));
	}

	private boolean unify(Node type0, Node type1) {
		return new BinderRecursive(new Trail()).bind(type0, type1);
		// return Binder.bind(type0, type1, new Trail());
	}

	private Node typeOf(Funp n) {
		var type = typeByNode.get(n);
		return type != null ? type.finalNode() : fail("no type information for '" + n + "'");
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
		Collection<Reference> structMembers;
		Node[] m, d;
		var n = n0.finalNode();
		if (n == typeBoolean)
			return booleanSize;
		else if ((m = typePatDecor.match(n)) != null)
			if ((d = typeDecorArray.match(m[0])) != null) {
				int size = Int.num(d[0]);
				return size != 0 ? getTypeSize(m[1]) * size : 0;
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
			return Read.from(structMembers).toInt(AsInt.sum(this::getTypeSize));
		else if ((m = typePatTag.match(n)) != null) {
			var dict = Dict.m(m[0]);
			var size = 0;
			for (var t : Read.from2(dict).values())
				size = max(size, getTypeSize(t));
			return size;
		} else
			return Funp_.fail(null, "cannot get size of type '" + toTypeString(n) + "'");
	}

	private Streamlet2<Node, Reference> isCompletedStructList(Node n) {
		var m = typePatStruct.match(n);
		if (m != null && m[0] == Atom.TRUE)
			return Tree.read(m[2]).map2(key -> {
				var reference = new Reference();
				var map = new HashMap<Node, Reference>();
				map.put(key, reference);
				unify(m[1], Dict.of(map));
				return reference;
			});
		else
			return null;
	}

	private Collection<Reference> isCompletedStructSet(Node n) {
		var m = typePatStruct.match(n);
		return m != null && m[0] == Atom.TRUE ? Dict.m(m[1]).values() : null;
	}

	private String toTypeString(Node type) {
		return toTypeString(PerSet.empty(), type);
	}

	private String toTypeString(PerSet<Integer> set0, Node n0) {
		Streamlet2<Node, Reference> pairs;
		Node[] m, d;
		var n = n0.finalNode();
		var hash = System.identityHashCode(n);

		if (!set0.contains(hash)) {
			var set = set0.add(hash);

			if (n == typeBoolean)
				return "boolean";
			else if ((m = typePatDecor.match(n)) != null)
				if ((d = typeDecorArray.match(m[0])) != null)
					if (d[0] instanceof Int i) {
						int size = i.number;
						return "[" + size + "]" + toTypeString(set, m[1]);
					} else if (d[0] instanceof Reference)
						return "[]" + toTypeString(set, m[1]);
					else
						return n.toString();
				else if ((d = typeDecorIo.match(m[0])) != null)
					return "io(" + toTypeString(set, m[1]) + ")";
				else if ((d = typeDecorRef.match(m[0])) != null)
					return "*" + toTypeString(set, m[1]);
				else
					return fail();
			else if ((m = typePatInt.match(n)) != null)
				return "n" + Int.num(m[0]);
			else if ((m = typePatLambda.match(n)) != null)
				return "(" + toTypeString(set, m[0]) + ") => " + toTypeString(set, m[1]);
			else if ((pairs = isCompletedStructList(n)) != null)
				return Read //
						.from2(pairs) //
						.map((k, v) -> k + ":" + toTypeString(set, v) + ", ") //
						.toJoinedString("{", "", "}");
			else if ((m = typePatStruct.match(n)) != null)
				return Read //
						.from2(Dict.m(m[1])) //
						.map((k, v) -> k + ":" + toTypeString(set, v) + ", ") //
						.sort(Compare::string) //
						.toJoinedString("{", "", m[0].finalNode() == Atom.TRUE ? "}" : "...}");
			else if ((m = typePatTag.match(n)) != null) {
				var dict = Dict.m(m[0]);
				return Read //
						.from2(dict) //
						.map((k, v) -> k + ":" + toTypeString(set, v) + ", ") //
						.sort(Compare::string) //
						.toJoinedString("<", "", ">");
			} else
				return n.toString();
		} else
			return "<<recurse>>";
	}

}
