package suite.funp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.Mutable;
import suite.adt.pair.Fixie_.FixieFun0;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.adt.pair.Pair;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.fp.Unify;
import suite.fp.Unify.UnNode;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpAsm;
import suite.funp.P0.FunpAssignReference;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpCheckType;
import suite.funp.P0.FunpCoerce;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpError;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpGlobal;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpIo;
import suite.funp.P0.FunpIoCat;
import suite.funp.P0.FunpIterate;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpLambdaCapture;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPredefine;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpRepeat;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpTree2;
import suite.funp.P0.FunpVariable;
import suite.funp.P2.FunpAllocGlobal;
import suite.funp.P2.FunpAllocStack;
import suite.funp.P2.FunpAssign;
import suite.funp.P2.FunpData;
import suite.funp.P2.FunpFramePointer;
import suite.funp.P2.FunpInvoke;
import suite.funp.P2.FunpInvoke2;
import suite.funp.P2.FunpInvokeIo;
import suite.funp.P2.FunpMemory;
import suite.funp.P2.FunpOperand;
import suite.funp.P2.FunpRoutine;
import suite.funp.P2.FunpRoutine2;
import suite.funp.P2.FunpRoutineIo;
import suite.funp.P2.FunpSaveRegisters;
import suite.funp.P2.FunpWhile;
import suite.immutable.IMap;
import suite.immutable.ISet;
import suite.inspect.Inspect;
import suite.node.io.TermOp;
import suite.node.util.Singleton;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.adt.pair.IntIntPair;
import suite.streamlet.Read;
import suite.util.AutoObject;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;
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

	private UnNode<Type> typeBoolean = TypeBoolean.of();
	private UnNode<Type> typeByte = TypeByte.of();
	private UnNode<Type> typeNumber = TypeNumber.of();
	private Map<Funp, UnNode<Type>> typeByNode = new HashMap<>();

	public Funp infer(Funp n0) {
		UnNode<Type> t = unify.newRef();
		Funp n1 = extractPredefine(n0);
		Funp n2 = Boolean.FALSE ? captureLambdas(n1) : n1;

		if (unify.unify(t, new Infer(IMap.empty()).infer(n2)))
			return new Erase(0, IMap.empty()).erase(n2);
		else
			return Fail.t("cannot infer type for " + n0);
	}

	private Funp extractPredefine(Funp node0) {
		List<Pair<String, Funp>> evs = new ArrayList<>();

		Funp node1 = new Object() {
			private Funp extract_(Funp n) {
				return inspect.rewrite(Funp.class, n_ -> {
					return n_.<Funp> switch_( //
					).applyIf(FunpDefine.class, f -> f.apply((isPolyType, var, value, expr) -> {
						return FunpDefine.of(isPolyType, var, extractPredefine(value), extract_(expr));
					})).applyIf(FunpDefineRec.class, f -> f.apply((pairs0, expr) -> {
						List<Pair<String, Funp>> pairs1 = Read.from2(pairs0).mapValue(P2InferType.this::extractPredefine).toList();
						return FunpDefineRec.of(pairs1, extract_(expr));
					})).applyIf(FunpGlobal.class, f -> f.apply((var, value, expr) -> {
						return FunpGlobal.of(var, extractPredefine(value), extract_(expr));
					})).applyIf(FunpLambda.class, f -> f.apply((var, expr) -> {
						return FunpLambda.of(var, extractPredefine(expr));
					})).applyIf(FunpPredefine.class, f -> f.apply(expr -> {
						var ev = "ev" + Util.temp();
						evs.add(Pair.of(ev, expr));
						Funp var = FunpVariable.of(ev);
						return FunpAssignReference.of(FunpReference.of(var), expr, var);
					})).result();
				}, n);
			}
		}.extract_(node0);

		for (Pair<String, Funp> pair : evs)
			node1 = FunpDefine.of(false, pair.t0, FunpDontCare.of(), node1);

		return node1;
	}

	private Funp captureLambdas(Funp node0) {
		class Capture {
			private Fun<String, Funp> accesses;
			private ISet<String> locals;
			private ISet<String> globals;

			private Capture(Fun<String, Funp> accesses, ISet<String> locals, ISet<String> globals) {
				this.accesses = accesses;
				this.locals = locals;
				this.globals = globals;
			}

			private Funp capture(Funp n) {
				return inspect.rewrite(Funp.class, this::capture_, n);
			}

			private Funp capture_(Funp n) {
				return n.<Funp> switch_( //
				).applyIf(FunpDefine.class, f -> f.apply((isPolyType, var, value, expr) -> {
					Capture c1 = new Capture(accesses, locals.add(var), globals);
					return FunpDefine.of(isPolyType, var, capture(value), c1.capture(expr));
				})).applyIf(FunpDefineRec.class, f -> f.apply((vars, expr) -> {
					List<Pair<String, Funp>> vars1 = new ArrayList<>();
					ISet<String> locals1 = locals;
					for (Pair<String, Funp> pair : vars) {
						locals1 = locals1.add(pair.t0);
						vars1.add(Pair.of(pair.t0, capture(pair.t1)));
					}
					Capture c1 = new Capture(accesses, locals1, globals);
					return FunpDefineRec.of(vars1, c1.capture(expr));
				})).applyIf(FunpGlobal.class, f -> f.apply((var, value, expr) -> {
					Capture c1 = new Capture(accesses, locals, globals.add(var));
					return FunpGlobal.of(var, capture(value), c1.capture(expr));
				})).applyIf(FunpIterate.class, f -> f.apply((var, init, cond, iterate) -> {
					Capture c1 = new Capture(accesses, locals.add(var), globals);
					return FunpIterate.of(var, capture(init), c1.capture(cond), c1.capture(iterate));
				})).applyIf(FunpLambda.class, f -> f.apply((var, expr) -> {
					ISet<String> locals1 = ISet.empty();
					var capn = "cap" + Util.temp();
					FunpVariable cap = FunpVariable.of(capn);
					FunpReference ref = FunpReference.of(cap);
					Set<String> set = new HashSet<>();
					List<Pair<String, Funp>> list = new ArrayList<>();
					FunpStruct struct = FunpStruct.of(list);

					Capture c1 = new Capture(v -> {
						if (set.add(v))
							list.add(Pair.of(v, FunpVariable.of(v)));
						return FunpField.of(ref, v);
					}, locals1.add(capn).add(var), globals);

					return FunpGlobal.of(capn, struct, FunpLambdaCapture.of(var, capn, cap, c1.capture(expr)));

					// TODO allocate cap on heap
					// TODO free cap after use
				})).applyIf(FunpVariable.class, f -> f.apply(var -> {
					return locals.contains(var) || globals.contains(var) ? f : accesses.apply(var);
				})).result();
			}
		}

		return new Capture(v -> Fail.t(), ISet.empty(), ISet.empty()).capture(node0);
	}

	private class Infer {
		private IMap<String, Pair<Boolean, UnNode<Type>>> env;

		private Infer(IMap<String, Pair<Boolean, UnNode<Type>>> env) {
			this.env = env;
		}

		private UnNode<Type> infer(Funp n) {
			UnNode<Type> t = typeByNode.get(n);
			if (t == null)
				typeByNode.put(n, t = infer_(n));
			return t;
		}

		private UnNode<Type> infer_(Funp n) {
			return new Switch<UnNode<Type>>(n //
			).applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
				UnNode<Type> tr = unify.newRef();
				unify(n, TypeLambda.of(infer(value), tr), infer(lambda));
				return tr;
			})).applyIf(FunpArray.class, f -> f.apply(elements -> {
				UnNode<Type> te = unify.newRef();
				for (Funp element : elements)
					unify(n, te, infer(element));
				return TypeArray.of(te, elements.size());
			})).applyIf(FunpAsm.class, f -> f.apply((assigns, asm) -> {
				for (Pair<OpReg, Funp> assign : assigns) {
					UnNode<Type> tp = infer(assign.t1);
					if (tp.final_() instanceof Type)
						if (assign.t0.size == getTypeSize(tp))
							;
						else
							return Fail.t();
					else if (assign.t0.size == 1)
						unify(n, typeByte, tp);
					else if (assign.t0.size == is)
						unify(n, typeNumber, tp);
					else
						return Fail.t();
				}
				return TypeIo.of(typeNumber);
			})).applyIf(FunpAssignReference.class, f -> f.apply((reference, value, expr) -> {
				unify(n, infer(reference), TypeReference.of(infer(value)));
				return infer(expr);
			})).applyIf(FunpBoolean.class, f -> {
				return typeBoolean;
			}).applyIf(FunpCheckType.class, f -> f.apply((left, right, expr) -> {
				unify(n, infer(left), infer(right));
				return infer(expr);
			})).applyIf(FunpCoerce.class, f -> f.apply((coerce, expr) -> {
				unify(n, typeNumber, infer(expr));
				return typeByte;
			})).applyIf(FunpDefine.class, f -> f.apply((isPolyType, var, value, expr) -> {
				return new Infer(env.replace(var, Pair.of(isPolyType, infer(value)))).infer(expr);
			})).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr) -> {
				for (Pair<String, Funp> pair : pairs)
					env = env.replace(pair.t0, Pair.of(true, infer(pair.t1)));
				return new Infer(env).infer(expr);
			})).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
				UnNode<Type> t = unify.newRef();
				unify(n, TypeReference.of(t), infer(pointer));
				return t;
			})).applyIf(FunpDontCare.class, f -> {
				return unify.newRef();
			}).applyIf(FunpError.class, f -> {
				return unify.newRef();
			}).applyIf(FunpField.class, f -> f.apply((reference, field) -> {
				UnNode<Type> tf = unify.newRef();
				TypeStruct ts = TypeStruct.of();
				ts.pairs.add(Pair.of(field, tf));
				unify(n, infer(reference), TypeReference.of(ts));
				return tf;
			})).applyIf(FunpGlobal.class, f -> f.apply((var, value, expr) -> {
				return new Infer(env.replace(var, Pair.of(false, infer(value)))).infer(expr);
			})).applyIf(FunpIf.class, f -> f.apply((if_, then, else_) -> {
				UnNode<Type> t;
				unify(n, typeBoolean, infer(if_));
				unify(n, t = infer(then), infer(else_));
				return t;
			})).applyIf(FunpIo.class, f -> f.apply(expr -> {
				return TypeIo.of(infer(expr));
			})).applyIf(FunpIoCat.class, f -> f.apply(expr -> {
				UnNode<Type> ta = unify.newRef();
				UnNode<Type> tb = unify.newRef();
				UnNode<Type> tbio = TypeIo.of(tb);
				unify(n, TypeLambda.of(ta, tbio), infer(expr));
				return TypeLambda.of(TypeIo.of(ta), tbio);
			})).applyIf(FunpIndex.class, f -> f.apply((reference, index) -> {
				UnNode<Type> te = unify.newRef();
				unify(n, TypeReference.of(TypeArray.of(te)), infer(reference));
				unify(n, infer(index), typeNumber);
				return te;
			})).applyIf(FunpIterate.class, f -> f.apply((var, init, cond, iterate) -> {
				UnNode<Type> tv = unify.newRef();
				UnNode<Type> tv1 = TypeIo.of(tv);
				Infer i1 = new Infer(env.replace(var, Pair.of(false, tv)));
				unify(n, tv, infer(init));
				unify(n, typeBoolean, i1.infer(cond));
				unify(n, tv1, i1.infer(iterate));
				return tv1;
			})).applyIf(FunpLambda.class, f -> f.apply((var, expr) -> {
				UnNode<Type> tv = unify.newRef();
				return TypeLambda.of(tv, new Infer(env.replace(var, Pair.of(false, tv))).infer(expr));
			})).applyIf(FunpLambdaCapture.class, f -> f.apply((var, capn, cap, expr) -> {
				UnNode<Type> tv = unify.newRef();
				IMap<String, Pair<Boolean, UnNode<Type>>> env0 = IMap.empty();
				IMap<String, Pair<Boolean, UnNode<Type>>> env1 = env0 //
						.replace(capn, Pair.of(false, infer(cap))) //
						.replace(var, Pair.of(false, tv));
				return TypeLambda.of(tv, new Infer(env1).infer(expr));
			})).applyIf(FunpNumber.class, f -> {
				return typeNumber;
			}).applyIf(FunpReference.class, f -> f.apply(expr -> {
				return TypeReference.of(infer(expr));
			})).applyIf(FunpRepeat.class, f -> f.apply((count, expr) -> {
				return TypeArray.of(infer(expr), count);
			})).applyIf(FunpStruct.class, f -> f.apply(pairs -> {
				return TypeStruct.of(Read.from2(pairs).mapValue(this::infer).toList());
			})).applyIf(FunpTree.class, f -> f.apply((op, left, right) -> {
				UnNode<Type> ti = op == TermOp.BIGAND || op == TermOp.BIGOR_ ? typeBoolean : typeNumber;
				unify(n, infer(left), ti);
				unify(n, infer(right), ti);
				if (op == TermOp.EQUAL_ || op == TermOp.NOTEQ_ || op == TermOp.LE____ || op == TermOp.LT____)
					return typeBoolean;
				else
					return ti;
			})).applyIf(FunpTree2.class, f -> f.apply((operator, left, right) -> {
				unify(n, infer(left), typeNumber);
				unify(n, infer(right), typeNumber);
				return typeNumber;
			})).applyIf(FunpVariable.class, f -> f.apply(var -> {
				Pair<Boolean, UnNode<Type>> pair = env.get(var);
				UnNode<Type> tv = pair.t1;
				return pair.t0 ? unify.clone(tv) : tv;
			})).nonNullResult();
		}
	}

	private class Erase {
		private int scope;
		private IMap<String, Var> env;

		private Erase(int scope, IMap<String, Var> env) {
			this.scope = scope;
			this.env = env;
		}

		private Funp erase(Funp n) {
			return inspect.rewrite(Funp.class, this::erase_, n);
		}

		private Funp erase_(Funp n) {
			Type type0 = typeOf(n);

			return n.<Funp> switch_( //
			).applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
				LambdaType lt = lambdaType(lambda);
				Funp lambda1 = erase(lambda);
				var size = getTypeSize(typeOf(value));
				Funp invoke;
				if (lt.os == is)
					invoke = allocStack(size, value, FunpInvoke.of(lambda1));
				else if (lt.os == ps * 2)
					invoke = allocStack(size, value, FunpInvoke2.of(lambda1));
				else
					invoke = allocStack(lt.os, FunpDontCare.of(), allocStack(size, value, FunpInvokeIo.of(lambda1)));
				return FunpSaveRegisters.of(invoke);
			})).applyIf(FunpArray.class, f -> f.apply(elements -> {
				UnNode<Type> te = unify.newRef();
				unify(n, type0, TypeArray.of(te));
				var elementSize = getTypeSize(te);
				var offset = 0;
				List<Pair<Funp, IntIntPair>> list = new ArrayList<>();
				for (Funp element : elements) {
					var offset0 = offset;
					list.add(Pair.of(erase(element), IntIntPair.of(offset0, offset += elementSize)));
				}
				return FunpData.of(list);
			})).applyIf(FunpAsm.class, f -> f.apply((assigns, asm) -> {
				return FunpSaveRegisters.of(FunpAsm.of(Read.from2(assigns).mapValue(this::erase).toList(), asm));
			})).applyIf(FunpAssignReference.class, f -> f.apply((reference, value, expr) -> {
				UnNode<Type> t = unify.newRef();
				unify(n, typeOf(reference), TypeReference.of(t));
				var size = getTypeSize(t);
				return FunpAssign.of(FunpMemory.of(erase(reference), 0, size), erase(value), erase(expr));
			})).applyIf(FunpCheckType.class, f -> f.apply((left, right, expr) -> {
				return erase(expr);
			})).applyIf(FunpDefine.class, f -> f.apply((isPolyType, var, value, expr) -> {
				Mutable<Integer> offset = Mutable.nil();
				var size0 = getTypeSize(typeOf(value));
				Erase e1 = new Erase(scope, env.replace(var, new Var(scope, offset, 0, size0)));
				return allocStack(size0, value, e1.erase(expr), offset);
			})).applyIf(FunpDefineRec.class, f -> f.apply((vars, expr) -> {
				List<Pair<Var, Funp>> assigns = new ArrayList<>();
				Mutable<Integer> offsetStack = Mutable.nil();
				IMap<String, Var> env1 = env;
				var offset = 0;

				for (Pair<String, Funp> pair : vars) {
					var offset0 = offset;
					Funp value = pair.t1;
					Var var = new Var(scope, offsetStack, offset0, offset += getTypeSize(typeOf(value)));
					env1 = env1.replace(pair.t0, var);
					assigns.add(Pair.of(var, value));
				}

				Erase e1 = new Erase(scope, env1);
				Funp expr_ = e1.erase(expr);

				for (Pair<Var, Funp> pair : assigns)
					expr = FunpAssign.of(e1.getVariable(pair.t0), e1.erase(pair.t1), expr);

				return FunpAllocStack.of(align(offset), FunpDontCare.of(), expr_, offsetStack);
			})).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
				return FunpMemory.of(erase(pointer), 0, getTypeSize(type0));
			})).applyIf(FunpField.class, f -> f.apply((reference, field) -> {
				TypeStruct ts = TypeStruct.of();
				unify(n, typeOf(reference), TypeReference.of(ts));
				TypeStruct ts1 = ts.finalStruct();
				var offset = 0;
				if (ts1.isCompleted)
					for (Pair<String, UnNode<Type>> pair : ts1.pairs) {
						var offset1 = offset + getTypeSize(pair.t1);
						if (!String_.equals(pair.t0, field))
							offset = offset1;
						else
							return FunpMemory.of(erase(reference), offset, offset1);
					}
				return Fail.t();
			})).applyIf(FunpGlobal.class, f -> f.apply((var, value, expr) -> {
				var size = getTypeSize(typeOf(value));
				Mutable<Operand> address = Mutable.nil();
				Erase e1 = new Erase(scope, env.replace(var, new Var(address, 0, size)));
				FunpAssign expr1 = FunpAssign.of(FunpMemory.of(FunpOperand.of(address), 0, size), erase(value), e1.erase(expr));
				return FunpAllocGlobal.of(var, size, expr1, address);
			})).applyIf(FunpIo.class, f -> f.apply(expr -> {
				return erase(expr);
			})).applyIf(FunpIoCat.class, f -> f.apply(expr -> {
				return erase(expr);
			})).applyIf(FunpIndex.class, f -> f.apply((reference, index) -> {
				UnNode<Type> te = unify.newRef();
				unify(n, typeOf(reference), TypeReference.of(TypeArray.of(te)));
				var size = getTypeSize(te);
				Funp address0 = erase(reference);
				FunpTree inc = FunpTree.of(TermOp.MULT__, erase(index), FunpNumber.ofNumber(size));
				Funp address1 = FunpTree.of(TermOp.PLUS__, address0, inc);
				return FunpMemory.of(address1, 0, size);
			})).applyIf(FunpIterate.class, f -> f.apply((var, init, cond, iterate) -> {
				Mutable<Integer> offset = Mutable.nil();
				var size = getTypeSize(typeOf(init));
				Var var_ = new Var(scope, offset, 0, size);
				Erase e1 = new Erase(scope, env.replace(var, var_));
				FunpMemory m = getVariable(var_);
				FunpWhile while_ = FunpWhile.of(e1.erase(cond), FunpAssign.of(m, e1.erase(iterate), FunpDontCare.of()), m);
				return allocStack(size, init, while_, offset);
			})).applyIf(FunpLambda.class, f -> f.apply((var, expr) -> {
				var b = ps * 2; // return address and EBP
				var scope1 = scope + 1;
				LambdaType lt = lambdaType(n);
				FunpFramePointer frame = Funp_.framePointer;
				Funp expr1 = new Erase(scope1, env.replace(var, new Var(scope1, Mutable.of(0), b, b + lt.is))).erase(expr);
				return eraseRoutine(lt, frame, expr1);
			})).applyIf(FunpLambdaCapture.class, f -> f.apply((var, capn, cap, expr) -> {
				var b = ps * 2; // return address and EBP
				LambdaType lt = lambdaType(n);
				var size = getTypeSize(typeOf(cap));
				IMap<String, Var> env0 = IMap.empty();
				IMap<String, Var> env1 = env0 //
						.replace(capn, new Var(0, Mutable.of(0), 0, size)) //
						.replace(var, new Var(1, Mutable.of(0), b, b + lt.is));
				Funp frame = FunpReference.of(erase(cap));
				Funp expr1 = new Erase(1, env1).erase(expr);
				return eraseRoutine(lt, frame, expr1);
			})).applyIf(FunpReference.class, f -> f.apply(expr -> {
				return new Object() {
					private Funp getAddress(Funp n) {
						return n.<Funp> switch_( //
						).applyIf(FunpAssign.class, f -> f.apply((memory, value, expr) -> {
							return FunpAssign.of(memory, value, getAddress(expr));
						})).applyIf(FunpMemory.class, f -> f.apply((pointer, start, end) -> {
							return FunpTree.of(TermOp.PLUS__, pointer, FunpNumber.ofNumber(start));
						})).applyIf(FunpVariable.class, f -> f.apply(var -> {
							return getAddress(getVariable(env.get(var)));
						})).nonNullResult();
					}
				}.getAddress(erase(expr));
			})).applyIf(FunpRepeat.class, f -> f.apply((count, expr) -> {
				var elementSize = getTypeSize(typeOf(expr));
				var offset = 0;
				List<Pair<Funp, IntIntPair>> list = new ArrayList<>();
				for (int i = 0; i < count; i++) {
					var offset0 = offset;
					list.add(Pair.of(expr, IntIntPair.of(offset0, offset += elementSize)));
				}
				return FunpData.of(list);
			})).applyIf(FunpStruct.class, f -> f.apply(fvs -> {
				TypeStruct ts0 = TypeStruct.of();
				unify(n, ts0, type0);

				TypeStruct ts1 = ts0.finalStruct();
				Map<String, Funp> values = Read.from2(fvs).toMap();
				List<Pair<Funp, IntIntPair>> list = new ArrayList<>();
				var offset = 0;

				if (ts1.isCompleted)
					for (Pair<String, UnNode<Type>> pair : ts1.pairs) {
						var offset0 = offset;
						list.add(Pair.of(erase(values.get(pair.t0)), IntIntPair.of(offset0, offset += getTypeSize(pair.t1))));
					}
				else
					Fail.t();

				return FunpData.of(list);
			})).applyIf(FunpVariable.class, f -> f.apply(var -> {
				return getVariable(env.get(var));
			})).result();
		}

		private Funp eraseRoutine(LambdaType lt, Funp frame, Funp expr) {
			if (lt.os == is)
				return FunpRoutine.of(frame, expr);
			else if (lt.os == ps * 2)
				return FunpRoutine2.of(frame, expr);
			else
				return FunpRoutineIo.of(frame, expr, lt.is, lt.os);
		}

		private FunpAllocStack allocStack(int size0, Funp value, Funp expr) {
			return allocStack(size0, value, expr, Mutable.nil());
		}

		private FunpAllocStack allocStack(int size0, Funp value, Funp expr, Mutable<Integer> stack) {
			return FunpAllocStack.of(align(size0), erase(value), expr, stack);
		}

		private FunpMemory getVariable(Var vd) {
			Mutable<Operand> operand = vd.operand;
			var scope0 = vd.scope;
			Funp nfp;
			if (scope0 != null) {
				nfp = Funp_.framePointer;
				for (int i = scope0; i < scope; i++)
					nfp = FunpMemory.of(nfp, 0, ps);
			} else
				nfp = FunpNumber.of(Mutable.of(0));
			if (operand != null)
				nfp = FunpTree.of(TermOp.PLUS__, nfp, FunpOperand.of(operand));
			return FunpMemory.of(FunpTree.of(TermOp.PLUS__, nfp, FunpNumber.of(vd.offset)), vd.start, vd.end);
		}

		private int align(int size0) {
			var is1 = is - 1;
			return (size0 + is1) & ~is1;
		}
	}

	private class Var {
		private Integer scope;
		private Mutable<Integer> offset;
		private Mutable<Operand> operand;
		private int start;
		private int end;

		public Var(Mutable<Operand> operand, int start, int end) {
			this.offset = Mutable.of(0);
			this.operand = operand;
			this.start = start;
			this.end = end;
		}

		public Var(int scope, Mutable<Integer> offset, int start, int end) {
			this.scope = scope;
			this.offset = offset;
			this.start = start;
			this.end = end;
		}
	}

	private LambdaType lambdaType(Funp lambda) {
		LambdaType lt = new LambdaType(lambda);
		return lt.os <= is || lt.os == ps * 2 ? lt : Fail.t();

	}

	private class LambdaType {
		private int is, os;

		private LambdaType(Funp lambda) {
			UnNode<Type> tp = unify.newRef();
			UnNode<Type> tr = unify.newRef();
			unify(lambda, typeOf(lambda), TypeLambda.of(tp, tr));
			is = getTypeSize(tp);
			os = getTypeSize(tr);
		}
	}

	private void unify(Funp n, UnNode<Type> type0, UnNode<Type> type1) {
		if (!unify.unify(type0, type1))
			Fail.t("cannot unify types in " + n + " between " + type0.final_() + " and " + type1.final_());
	}

	private Type typeOf(Funp n) {
		return (Type) typeByNode.get(n).final_();
	}

	private int getTypeSize(UnNode<Type> n) {
		var result = new Switch<Integer>(n.final_() //
		).applyIf(TypeArray.class, t -> t.apply((elementType, size) -> {
			return getTypeSize(elementType) * size;
		})).applyIf(TypeBoolean.class, t -> t.apply(() -> {
			return Funp_.booleanSize;
		})).applyIf(TypeByte.class, t -> t.apply(() -> {
			return 1;
		})).applyIf(TypeIo.class, t -> t.apply(type -> {
			return getTypeSize(type);
		})).applyIf(TypeLambda.class, t -> t.apply((parameterType, returnType) -> {
			return ps + ps;
		})).applyIf(TypeNumber.class, t -> t.apply(() -> {
			return is;
		})).applyIf(TypeReference.class, t -> t.apply(type -> {
			return ps;
		})).applyIf(TypeStruct.class, t -> t.apply(pairs -> {
			return Read.from(pairs).toInt(Obj_Int.sum(field -> getTypeSize(field.t1)));
		})).result();

		return result != null ? result.intValue() : Fail.t("cannot get size of type " + n);
	}

	private static Unify<Type> unify = new Unify<>();

	private static class TypeArray extends Type {
		private int id = Util.temp();
		private TypeArray ref = this;
		private UnNode<Type> elementType;
		private int size;

		private static TypeArray of(UnNode<Type> elementType) {
			return TypeArray.of(elementType, -1);
		}

		private static TypeArray of(UnNode<Type> elementType, int size) {
			TypeArray t = new TypeArray();
			t.elementType = elementType;
			t.size = size;
			return t;
		}

		private <R> R apply(FixieFun2<UnNode<Type>, Integer, R> fun) {
			TypeArray ta = finalArray();
			return fun.apply(ta.elementType, ta.size);
		}

		public boolean unify(UnNode<Type> type) {
			boolean b = getClass() == type.getClass();

			if (b) {
				TypeArray x = finalArray();
				TypeArray y = ((TypeArray) type).finalArray();
				boolean ord = x.id < y.id;
				TypeArray ta0 = ord ? x : y;
				TypeArray ta1 = ord ? y : x;

				if (ta0.size == -1)
					ta0.size = ta1.size;
				else if (ta1.size == -1)
					ta1.size = ta0.size;

				b &= unify.unify(ta0.elementType, ta1.elementType) && ta0.size == ta1.size;

				if (b)
					ta1.ref = ta0;
			}

			return b;
		}

		private TypeArray finalArray() {
			return ref != this ? ref.finalArray() : this;
		}
	}

	private static class TypeBoolean extends Type {
		private static TypeBoolean of() {
			return new TypeBoolean();
		}

		private <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
		}
	}

	private static class TypeByte extends Type {
		private static TypeByte of() {
			return new TypeByte();
		}

		private <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
		}
	}

	private static class TypeIo extends Type {
		private UnNode<Type> type;

		private static TypeIo of(UnNode<Type> type) {
			TypeIo t = new TypeIo();
			t.type = type;
			return t;
		}

		private <R> R apply(FixieFun1<UnNode<Type>, R> fun) {
			return fun.apply(type);
		}
	}

	private static class TypeLambda extends Type {
		private UnNode<Type> parameterType, returnType;

		private static TypeLambda of(UnNode<Type> parameterType, UnNode<Type> returnType) {
			TypeLambda t = new TypeLambda();
			t.parameterType = parameterType;
			t.returnType = returnType;
			return t;
		}

		private <R> R apply(FixieFun2<UnNode<Type>, UnNode<Type>, R> fun) {
			return fun.apply(parameterType, returnType);
		}
	}

	private static class TypeNumber extends Type {
		private static TypeNumber of() {
			return new TypeNumber();
		}

		private <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
		}
	}

	private static class TypeReference extends Type {
		private UnNode<Type> type;

		private static TypeReference of(UnNode<Type> type) {
			TypeReference t = new TypeReference();
			t.type = type;
			return t;
		}

		private <R> R apply(FixieFun1<UnNode<Type>, R> fun) {
			return fun.apply(type);
		}
	}

	private static class TypeStruct extends Type {
		private int id = Util.temp();
		private TypeStruct ref = this;
		private List<Pair<String, UnNode<Type>>> pairs;
		private boolean isCompleted;

		private static TypeStruct of() {
			return new TypeStruct(new ArrayList<>(), false);
		}

		private static TypeStruct of(List<Pair<String, UnNode<Type>>> pairs) {
			return new TypeStruct(pairs, true);
		}

		private TypeStruct() {
		}

		private TypeStruct(List<Pair<String, UnNode<Type>>> pairs, boolean isCompleted) {
			this.pairs = pairs;
			this.isCompleted = isCompleted;
		}

		private <R> R apply(FixieFun1<List<Pair<String, UnNode<Type>>>, R> fun) {
			return fun.apply(finalStruct().pairs);
		}

		public boolean unify(UnNode<Type> type) {
			boolean b = getClass() == type.getClass();

			if (b) {
				TypeStruct x = finalStruct();
				TypeStruct y = ((TypeStruct) type).finalStruct();
				boolean ord = x.id < y.id;
				TypeStruct ts0 = ord ? x : y;
				TypeStruct ts1 = ord ? y : x;
				Map<String, UnNode<Type>> typeByField0 = Read.from2(ts0.pairs).toMap();
				Map<String, UnNode<Type>> typeByField1 = Read.from2(ts1.pairs).toMap();

				for (Pair<String, UnNode<Type>> e : ts1.pairs) {
					var field = e.t0;
					UnNode<Type> type0 = typeByField0.get(field);
					UnNode<Type> type1 = e.t1;
					if (type0 != null)
						b &= unify.unify(type0, type1);
					else {
						b &= !ts0.isCompleted;
						ts0.pairs.add(Pair.of(field, type1));
					}
				}

				b &= !ts1.isCompleted || Read.from2(ts0.pairs).keys().isAll(typeByField1::containsKey);

				if (b) {
					ts0.isCompleted |= ts1.isCompleted;
					ts1.ref = ts0;
				}
			}

			return b;
		}

		private TypeStruct finalStruct() {
			return ref != this ? ref.finalStruct() : this;
		}
	}

	private static class Type extends AutoObject<Type> implements UnNode<Type> {
		public boolean unify(UnNode<Type> type) {
			return getClass() == type.getClass() //
					&& fields().isAll(field -> Rethrow.ex(() -> unify.unify(cast(field.get(this)), cast(field.get(type)))));
		}

		private static UnNode<Type> cast(Object object) {
			@SuppressWarnings("unchecked")
			UnNode<Type> node = (UnNode<Type>) object;
			return node;
		}
	}

}
