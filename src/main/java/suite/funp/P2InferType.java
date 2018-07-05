package suite.funp;

import static suite.util.Friends.rethrow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import suite.adt.Mutable;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.adt.pair.Pair;
import suite.assembler.Amd64.Operand;
import suite.fp.Unify;
import suite.fp.Unify.UnNode;
import suite.funp.Funp_.CompileException;
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
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpError;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpIo;
import suite.funp.P0.FunpIoAsm;
import suite.funp.P0.FunpIoAssignRef;
import suite.funp.P0.FunpIoCat;
import suite.funp.P0.FunpIoFold;
import suite.funp.P0.FunpIoMap;
import suite.funp.P0.FunpIoWhile;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPredefine;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpRepeat;
import suite.funp.P0.FunpSizeOf;
import suite.funp.P0.FunpStruct;
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
import suite.immutable.ISet;
import suite.inspect.Inspect;
import suite.node.io.TermOp;
import suite.node.util.Singleton;
import suite.object.AutoObject;
import suite.primitive.ChrMutable;
import suite.primitive.IntMutable;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.IntIntPair;
import suite.streamlet.As;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.Read;
import suite.util.Fail;
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

	private UnNode<Type> typeBoolean = new TypeBoolean();
	private UnNode<Type> typeByte = new TypeByte();
	private UnNode<Type> typeNumber = new TypeNumber();

	private Map<Funp, UnNode<Type>> typeByNode = new IdentityHashMap<>();
	private Map<Funp, Boolean> isRegByNode = new IdentityHashMap<>();

	public Funp infer(Funp n0) {
		var t = unify.newRef();
		var n1 = extractPredefine(n0);
		var n2 = captureLambdas(n1);

		if (unify.unify(t, new Infer(IMap.empty()).infer(n2))) {
			var erase = new Erase(0, IMap.empty());
			erase.erase(n2); // first pass
			return erase.erase(n2); // second pass
		} else
			return Funp_.fail("cannot infer type for " + n0);
	}

	private Funp extractPredefine(Funp node0) {
		var evs = new ArrayList<Pair<String, Funp>>();

		var node1 = new Object() {
			private Funp extract(Funp n) {
				return inspect.rewrite(n, Funp.class, n_ -> {
					return n_.sw( //
					).applyIf(FunpDefine.class, f -> f.apply((type, var, value, expr) -> {
						return FunpDefine.of(type, var, extractPredefine(value), extract(expr));
					})).applyIf(FunpDefineRec.class, f -> f.apply((pairs0, expr) -> {
						var pairs1 = Read.from2(pairs0).mapValue(P2InferType.this::extractPredefine).toList();
						return FunpDefineRec.of(pairs1, extract(expr));
					})).applyIf(FunpLambda.class, f -> f.apply((var, expr) -> {
						return FunpLambda.of(var, extractPredefine(expr));
					})).applyIf(FunpPredefine.class, f -> f.apply(expr -> {
						var ev = "ev$" + Util.temp();
						evs.add(Pair.of(ev, expr));
						var var = FunpVariable.of(ev);
						return FunpIoAssignRef.of(FunpReference.of(var), expr, var);
					})).result();
				});
			}
		}.extract(node0);

		return Read //
				.from(evs) //
				.fold(node1, (n, pair) -> FunpDefine.of(Fdt.MONO, pair.t0, FunpDontCare.of(), n));
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
				return inspect.rewrite(n, Funp.class, this::capture_);
			}

			private Funp capture_(Funp n) {
				return n.sw( //
				).applyIf(FunpDefine.class, f -> f.apply((type, var, value, expr) -> {
					Capture c1;
					if (type != Fdt.GLOB)
						c1 = new Capture(accesses, locals.add(var), globals);
					else
						c1 = new Capture(accesses, locals, globals.add(var));
					return FunpDefine.of(type, var, capture(value), c1.capture(expr));
				})).applyIf(FunpDefineRec.class, f -> f.apply((vars, expr) -> {
					var locals1 = Read.from(vars).fold(locals, (l, pair) -> l.add(pair.t0));
					var c1 = new Capture(accesses, locals1, globals);
					var vars1 = new ArrayList<Pair<String, Funp>>();
					for (var pair : vars)
						vars1.add(Pair.of(pair.t0, c1.capture(pair.t1)));
					return FunpDefineRec.of(vars1, c1.capture(expr));
				})).applyIf(FunpLambda.class, f -> f.apply((var, expr) -> {
					if (Boolean.TRUE) // perform capture?
						return FunpLambda.of(var, new Capture(accesses, locals.replace(var), globals).capture(expr));
					else {
						var locals1 = ISet.<String> empty();
						var capn = "cap$" + Util.temp();
						var cap = FunpVariable.of(capn);
						var ref = FunpReference.of(cap);
						var set = new HashSet<>();
						var list = new ArrayList<Pair<String, Funp>>();
						var struct = FunpStruct.of(list);

						var c1 = new Capture(v -> {
							if (set.add(v))
								list.add(Pair.of(v, FunpVariable.of(v)));
							return FunpField.of(ref, v);
						}, locals1.add(capn).add(var), globals);

						return FunpDefine.of(Fdt.GLOB, capn, struct, FunpLambdaCapture.of(var, capn, cap, c1.capture(expr)));
					}

					// TODO allocate cap on heap
					// TODO free cap after use
				})).applyIf(FunpVariable.class, f -> f.apply(var -> {
					return locals.contains(var) || globals.contains(var) ? f : accesses.apply(var);
				})).result();
			}
		}

		return new Capture(Fail::t, ISet.empty(), ISet.empty()).capture(node0);
	}

	private class Infer {
		private IMap<String, Pair<Boolean, UnNode<Type>>> env;

		private Infer(IMap<String, Pair<Boolean, UnNode<Type>>> env) {
			this.env = env;
		}

		private UnNode<Type> infer(Funp n, String var) {
			try {
				return infer(n);
			} catch (CompileException ex) {
				return ex.rethrow(var);
			}
		}

		private UnNode<Type> infer(Funp n) {
			var t = typeByNode.get(n);
			if (t == null)
				typeByNode.put(n, t = infer_(n));
			return t;
		}

		private UnNode<Type> infer_(Funp n) {
			return new Switch<UnNode<Type>>(n //
			).applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
				var tr = unify.newRef();
				unify(n, TypeLambda.of(infer(value), tr), infer(lambda));
				return tr;
			})).applyIf(FunpArray.class, f -> f.apply(elements -> {
				var te = unify.newRef();
				for (var element : elements)
					unify(n, te, infer(element));
				return TypeArray.of(te, elements.size());
			})).applyIf(FunpBoolean.class, f -> {
				return typeBoolean;
			}).applyIf(FunpCheckType.class, f -> f.apply((left, right, expr) -> {
				unify(n, infer(left), infer(right));
				return infer(expr);
			})).applyIf(FunpCoerce.class, f -> f.apply((coerce, expr) -> {
				unify(n, typeNumber, infer(expr));
				if (coerce == Coerce.BYTE)
					return typeByte;
				else if (coerce == Coerce.NUMBER)
					return typeNumber;
				else if (coerce == Coerce.POINTER)
					return TypeReference.of(unify.newRef());
				else
					return Fail.t();
			})).applyIf(FunpDefine.class, f -> f.apply((type, var, value, expr) -> {
				return new Infer(env.replace(var, Pair.of(type == Fdt.POLY, infer(value, var)))).infer(expr);
			})).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr) -> {
				var pairs_ = Read.from(pairs);
				var env1 = pairs_.fold(env, (e, pair) -> e.put(pair.t0, Pair.of(false, unify.newRef())));
				var infer1 = new Infer(env1);
				for (var pair : pairs_) {
					var var = pair.t0;
					unify(n, env1.get(var).t1, infer1.infer(pair.t1, var));
				}
				return infer1.infer(expr);
			})).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
				var t = unify.newRef();
				unify(n, TypeReference.of(t), infer(pointer));
				return t;
			})).applyIf(FunpDontCare.class, f -> {
				return unify.newRef();
			}).applyIf(FunpError.class, f -> {
				return unify.newRef();
			}).applyIf(FunpField.class, f -> f.apply((reference, field) -> {
				var tf = unify.newRef();
				var ts = TypeStruct.of();
				ts.pairs.add(Pair.of(field, tf));
				unify(n, infer(reference), TypeReference.of(ts));
				return tf;
			})).applyIf(FunpIf.class, f -> f.apply((if_, then, else_) -> {
				UnNode<Type> t;
				unify(n, typeBoolean, infer(if_));
				unify(n, t = infer(then), infer(else_));
				return t;
			})).applyIf(FunpIo.class, f -> f.apply(expr -> {
				return TypeIo.of(infer(expr));
			})).applyIf(FunpIoAsm.class, f -> f.apply((assigns, asm) -> {
				for (var assign : assigns) {
					var tp = infer(assign.t1);
					if (tp.final_() instanceof Type)
						if (assign.t0.size == getTypeSize(tp))
							;
						else
							return Fail.t();
					else if (assign.t0.size == Funp_.booleanSize)
						unify(n, typeByte, tp);
					else if (assign.t0.size == is)
						unify(n, typeNumber, tp);
					else
						return Fail.t();
				}
				return TypeIo.of(typeNumber);
			})).applyIf(FunpIoAssignRef.class, f -> f.apply((reference, value, expr) -> {
				unify(n, infer(reference), TypeReference.of(infer(value)));
				return infer(expr);
			})).applyIf(FunpIoCat.class, f -> f.apply(expr -> {
				var ta = unify.newRef();
				var tb = unify.newRef();
				var tbio = TypeIo.of(tb);
				unify(n, TypeLambda.of(ta, tbio), infer(expr));
				return TypeLambda.of(TypeIo.of(ta), tbio);
			})).applyIf(FunpIoFold.class, f -> f.apply((init, cont, next) -> {
				var tv = unify.newRef();
				var tvio = TypeIo.of(tv);
				unify(n, tv, infer(init));
				unify(n, TypeLambda.of(tv, typeBoolean), infer(cont));
				unify(n, TypeLambda.of(tv, tvio), infer(next));
				return tvio;
			})).applyIf(FunpIoMap.class, f -> f.apply(expr -> {
				var ta = unify.newRef();
				var tb = unify.newRef();
				unify(n, TypeLambda.of(ta, tb), infer(expr));
				return TypeLambda.of(TypeIo.of(ta), TypeIo.of(tb));
			})).applyIf(FunpIoWhile.class, f -> f.apply((while_, do_, expr) -> {
				unify(n, typeBoolean, infer(while_));
				var td = infer(do_);
				if (td.final_() == td)
					unify(n, td, typeBoolean); // enforces a type to prevent
												// exception
				return infer(expr);
			})).applyIf(FunpIndex.class, f -> f.apply((reference, index) -> {
				var te = unify.newRef();
				unify(n, TypeReference.of(TypeArray.of(te)), infer(reference));
				unify(n, infer(index), typeNumber);
				return te;
			})).applyIf(FunpLambda.class, f -> f.apply((var, expr) -> {
				var tv = unify.newRef();
				return TypeLambda.of(tv, new Infer(env.replace(var, Pair.of(false, tv))).infer(expr));
			})).applyIf(FunpLambdaCapture.class, f -> f.apply((var, capn, cap, expr) -> {
				var tv = unify.newRef();
				var env0 = IMap.<String, Pair<Boolean, UnNode<Type>>> empty();
				var env1 = env0 //
						.replace(capn, Pair.of(false, infer(cap))) //
						.replace(var, Pair.of(false, tv));
				return TypeLambda.of(tv, new Infer(env1).infer(expr));
			})).applyIf(FunpNumber.class, f -> {
				return typeNumber;
			}).applyIf(FunpReference.class, f -> f.apply(expr -> {
				return TypeReference.of(infer(expr));
			})).applyIf(FunpRepeat.class, f -> f.apply((count, expr) -> {
				return TypeArray.of(infer(expr), count);
			})).applyIf(FunpSizeOf.class, f -> f.apply(expr -> {
				infer(expr);
				return typeNumber;
			})).applyIf(FunpStruct.class, f -> f.apply(pairs -> {
				var types = new ArrayList<Pair<String, UnNode<Type>>>();
				for (var kv : pairs) {
					var name = kv.t0;
					types.add(Pair.of(name, infer(kv.t1, name)));
				}
				return TypeStruct.of(types);
			})).applyIf(FunpTree.class, f -> f.apply((op, lhs, rhs) -> {
				UnNode<Type> ti;
				if (op == TermOp.BIGAND || op == TermOp.BIGOR_)
					ti = typeBoolean;
				else if (op == TermOp.EQUAL_ || op == TermOp.NOTEQ_)
					ti = unify.newRef();
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
			})).applyIf(FunpVariable.class, f -> f.apply(var -> env.get(var).map((isPolyType, tv) -> {
				return isPolyType ? unify.clone(tv) : tv;
			}))).applyIf(FunpVariableNew.class, f -> f.apply(var -> {
				return Funp_.fail("Undefined variable " + var);
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
			return inspect.rewrite(n, Funp.class, this::erase_);
		}

		private Funp erase_(Funp n) {
			var type0 = typeOf(n);

			return n.sw( //
			).applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
				var size = getTypeSize(typeOf(value));
				return apply(erase(value), lambda, size);
			})).applyIf(FunpArray.class, f -> f.apply(elements -> {
				var te = unify.newRef();
				unify(n, type0, TypeArray.of(te));
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
			})).applyIf(FunpDefine.class, f -> f.apply((type, var, value, expr) -> {
				if (type != Fdt.GLOB) {
					var size = getTypeSize(typeOf(value));
					var op = Mutable.<Operand> nil();
					var offset = IntMutable.nil();
					Var vd = new Var(f, op, scope, offset, null, 0, size);
					var e1 = new Erase(scope, env.replace(var, vd));
					var value1 = erase(value);
					var expr1 = e1.erase(expr);

					// if erase is called twice,
					// pass 1: check for any reference accesses to locals, set
					// isRegByNode;
					// pass 2: put locals to registers according to isRegByNode.
					var n1 = vd.isReg() ? FunpAllocReg.of(size, value1, expr1, op) : FunpAllocStack.of(size, value1, expr1, offset);

					isRegByNode.putIfAbsent(f, size == is);
					return n1;
				} else {
					var size = getTypeSize(typeOf(value));
					var address = Mutable.<Operand> nil();
					var e1 = new Erase(scope, env.replace(var, new Var(address, 0, size)));
					var expr1 = FunpAssignMem.of(FunpMemory.of(FunpOperand.of(address), 0, size), erase(value), e1.erase(expr));
					return FunpAllocGlobal.of(var, size, expr1, address);
				}
			})).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr) -> {
				var assigns = new ArrayList<Pair<Var, Funp>>();
				var offsetStack = IntMutable.nil();
				var env1 = env;
				var offset = 0;

				for (var pair : pairs) {
					var offset0 = offset;
					var value = pair.t1;
					var var = new Var(scope, offsetStack, offset0, offset += getTypeSize(typeOf(value)));
					env1 = env1.replace(pair.t0, var);
					assigns.add(Pair.of(var, value));
				}

				var e1 = new Erase(scope, env1);
				var expr1 = e1.erase(expr);

				var expr2 = Read //
						.from(assigns) //
						.fold(expr1, (e, pair) -> assign(e1.getVariable(pair.t0), e1.erase(pair.t1), e));

				return FunpAllocStack.of(offset, FunpDontCare.of(), expr2, offsetStack);
			})).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
				return FunpMemory.of(erase(pointer), 0, getTypeSize(type0));
			})).applyIf(FunpField.class, f -> f.apply((reference, field) -> {
				var ts = TypeStruct.of();
				unify(n, typeOf(reference), TypeReference.of(ts));
				var ts1 = ts.finalStruct();
				var offset = 0;
				if (ts1.isCompleted)
					for (var pair : ts1.pairs) {
						var offset1 = offset + getTypeSize(pair.t1);
						if (!String_.equals(pair.t0, field))
							offset = offset1;
						else
							return FunpMemory.of(erase(reference), offset, offset1);
					}
				return Fail.t();
			})).applyIf(FunpIo.class, f -> f.apply(expr -> {
				return erase(expr);
			})).applyIf(FunpIoAsm.class, f -> f.apply((assigns, asm) -> {
				env // disable register locals
						.streamlet2() //
						.values() //
						.filter(var -> var.scope != null && var.scope == scope) //
						.sink(var -> var.setReg(false));

				return FunpSaveRegisters.of(FunpIoAsm.of(Read.from2(assigns).mapValue(this::erase).toList(), asm));
			})).applyIf(FunpIoAssignRef.class, f -> f.apply((reference, value, expr) -> {
				return FunpAssignMem.of(memory(reference, n), erase(value), erase(expr));
			})).applyIf(FunpIoCat.class, f -> f.apply(expr -> {
				return erase(expr);
			})).applyIf(FunpIoFold.class, f -> f.apply((init, cont, next) -> {
				var offset = IntMutable.nil();
				var size = getTypeSize(typeOf(init));
				var var_ = new Var(scope, offset, 0, size);
				var e1 = new Erase(scope, env.replace("fold$" + Util.temp(), var_));
				var m = getVariable(var_);
				var cont_ = e1.apply(m, cont, size);
				var next_ = e1.apply(m, next, size);
				var while_ = FunpIoWhile.of(cont_, assign(m, next_, FunpDontCare.of()), m);
				return FunpAllocStack.of(size, e1.erase(init), while_, offset);
			})).applyIf(FunpIoMap.class, f -> f.apply(expr -> {
				return erase(expr);
			})).applyIf(FunpIndex.class, f -> f.apply((reference, index) -> {
				var te = unify.newRef();
				unify(n, typeOf(reference), TypeReference.of(TypeArray.of(te)));
				var size = getTypeSize(te);
				var address0 = erase(reference);
				var inc = FunpTree.of(TermOp.MULT__, erase(index), FunpNumber.ofNumber(size));
				var address1 = FunpTree.of(TermOp.PLUS__, address0, inc);
				return FunpMemory.of(address1, 0, size);
			})).applyIf(FunpLambda.class, f -> f.apply((var, expr) -> {
				var b = ps + ps; // return address and EBP
				var scope1 = scope + 1;
				var lt = new LambdaType(n);
				var frame = Funp_.framePointer;
				var expr1 = new Erase(scope1, env.replace(var, new Var(scope1, IntMutable.of(0), b, b + lt.is))).erase(expr);
				return eraseRoutine(lt, frame, expr1);
			})).applyIf(FunpLambdaCapture.class, f -> f.apply((var, capn, cap, expr) -> {
				var b = ps + ps; // return address and EBP
				var lt = new LambdaType(n);
				var size = getTypeSize(typeOf(cap));
				var env0 = IMap.<String, Var> empty();
				var env1 = env0 //
						.replace(capn, new Var(0, IntMutable.of(0), 0, size)) //
						.replace(var, new Var(1, IntMutable.of(0), b, b + lt.is));
				var frame = FunpReference.of(erase(cap));
				var expr1 = new Erase(1, env1).erase(expr);
				return eraseRoutine(lt, frame, expr1);
			})).applyIf(FunpReference.class, f -> f.apply(expr -> {
				return new Object() {
					private Funp getAddress(Funp n) {
						return n.sw( //
						).applyIf(FunpIoAssignRef.class, f -> f.apply((reference, value, expr) -> {
							return FunpAssignMem.of(memory(reference, f), erase(value), getAddress(expr));
						})).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
							return erase(pointer);
						})).applyIf(FunpVariable.class, f -> f.apply(var -> {
							var m = getVariableMemory(env.get(var));
							return m.apply((p, s, e) -> FunpTree.of(TermOp.PLUS__, p, FunpNumber.ofNumber(s)));
						})).nonNullResult();
					}
				}.getAddress(expr);
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
			})).applyIf(FunpStruct.class, f -> f.apply(fvs -> {
				var ts0 = TypeStruct.of();
				unify(n, ts0, type0);

				var ts1 = ts0.finalStruct();
				var values = Read.from2(fvs).toMap();
				var list = new ArrayList<Pair<Funp, IntIntPair>>();
				var offset = 0;

				if (ts1.isCompleted)
					for (var pair : ts1.pairs) {
						var offset0 = offset;
						list.add(Pair.of(erase(values.get(pair.t0)), IntIntPair.of(offset0, offset += getTypeSize(pair.t1))));
					}
				else
					Fail.t();

				return FunpData.of(list);
			})).applyIf(FunpTree.class, f -> f.apply((op, l, r) -> {
				var size0 = getTypeSize(typeOf(l));
				var size1 = getTypeSize(typeOf(r));
				if ((op == TermOp.EQUAL_ || op == TermOp.NOTEQ_) && (is < size0 || is < size1)) {
					var offsetStack = IntMutable.nil();
					var m0 = getVariableMemory(new Var(scope, offsetStack, 0, size0));
					var m1 = getVariableMemory(new Var(scope, offsetStack, size0, size0 + size1));
					var f0 = FunpCmp.of(op, m0, m1);
					var f1 = FunpAssignMem.of(m0, erase(l), f0);
					var f2 = FunpAssignMem.of(m1, erase(r), f1);
					return FunpAllocStack.of(size0 + size1, FunpDontCare.of(), f2, offsetStack);
				} else
					return null;
			})).applyIf(FunpVariable.class, f -> f.apply(var -> {
				return getVariable(env.get(var));
			})).result();
		}

		private Funp assign(Funp var, Funp value, Funp expr) {
			return var //
					.sw() //
					.applyIf(FunpMemory.class, f -> FunpAssignMem.of(f, value, expr)) //
					.applyIf(FunpOperand.class, f -> FunpAssignOp.of(f, value, expr)) //
					.nonNullResult();
		}

		private FunpMemory memory(FunpReference reference, Funp n) {
			var t = unify.newRef();
			unify(n, typeOf(reference), TypeReference.of(t));
			return FunpMemory.of(erase(reference), 0, getTypeSize(t));
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

		private FunpAllocStack allocStack(int size, Funp value, Funp expr) {
			return FunpAllocStack.of(size, value, expr, IntMutable.nil());
		}

		private Funp eraseRoutine(LambdaType lt, Funp frame, Funp expr) {
			if (lt.os == is)
				return FunpRoutine.of(frame, expr);
			else if (lt.os == ps * 2)
				return FunpRoutine2.of(frame, expr);
			else
				return FunpRoutineIo.of(frame, expr, lt.is, lt.os);
		}

		private Funp getVariable(Var vd) {
			var scope0 = vd.scope;
			vd.setReg(scope0 != null && scope0 == scope);
			return vd.isReg() ? FunpOperand.of(vd.operand) : getVariable_(vd);
		}

		private FunpMemory getVariableMemory(Var vd) {
			vd.setReg(false);
			return getVariable_(vd);
		}

		private FunpMemory getVariable_(Var vd) {
			var offsetOperand = vd.offsetOperand;
			var scope0 = vd.scope;
			var nfp0 = scope0 != null //
					? Ints_.range(scope0, scope).<Funp> fold(Funp_.framePointer, (i, n) -> FunpMemory.of(n, 0, ps)) // locals
					: FunpNumber.of(IntMutable.of(0)); // globals
			var nfp1 = offsetOperand != null ? FunpTree.of(TermOp.PLUS__, nfp0, FunpOperand.of(offsetOperand)) : nfp0;
			return FunpMemory.of(FunpTree.of(TermOp.PLUS__, nfp1, FunpNumber.of(vd.offset)), vd.start, vd.end);
		}
	}

	private class Var {
		private Funp funp;
		private Mutable<Operand> operand;
		private Integer scope;
		private IntMutable offset;
		private Mutable<Operand> offsetOperand;
		private int start, end;

		private Var(Mutable<Operand> offsetOperand, int start, int end) { // global
			this(FunpDontCare.of(), null, null, IntMutable.of(0), offsetOperand, start, end);
		}

		private Var(int scope, IntMutable offset, int start, int end) { // local
			this(FunpDontCare.of(), null, scope, offset, null, start, end);
		}

		private Var( //
				Funp funp, //
				Mutable<Operand> operand, //
				Integer scope, //
				IntMutable offset, //
				Mutable<Operand> offsetOperand, //
				int start, int end) {
			this.funp = funp;
			this.operand = operand;
			this.scope = scope;
			this.offset = offset;
			this.offsetOperand = offsetOperand;
			this.start = start;
			this.end = end;
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
			var tp = unify.newRef();
			var tr = unify.newRef();
			unify(lambda, typeOf(lambda), TypeLambda.of(tp, tr));
			is = getTypeSize(tp);
			os = getTypeSize(tr);
		}
	}

	private void unify(Funp n, UnNode<Type> type0, UnNode<Type> type1) {
		if (!unify.unify(type0, type1))
			Funp_.fail("" //
					+ "cannot unify types between:" //
					+ "\n<<< " + toString(type0) //
					+ "\n>>> " + toString(type1) //
					+ "\nin " + n.getClass().getSimpleName());
	}

	private String toString(UnNode<Type> type) {
		var ins = new IdentityHashMap<Object, Boolean>();
		var aliases = new IdentityHashMap<Object, Character>();
		var alias = ChrMutable.of('a');

		return new Object() {
			private String toString(UnNode<Type> type) {
				if (ins.put(type, true) == null)
					try {
						return new Switch<String>(type.final_() //
						).applyIf(TypeArray.class, t -> {
							return toString(t.elementType) + " * " + t.size;
						}).applyIf(TypeIo.class, t -> {
							return "io " + toString(t.type);
						}).applyIf(TypeLambda.class, t -> {
							return toString(t.parameterType) + " -> " + toString(t.returnType);
						}).applyIf(TypeReference.class, t -> {
							return "&" + toString(t.type);
						}).applyIf(TypeStruct.class, t -> {
							var s = t.isCompleted ? "" : "...";
							return Read.from(t.pairs).map(pair -> pair.t0 + ":" + toString(pair.t1) + ", ")
									.collect(As.joinedBy("(", "", s + ")"));
						}).applyIf(Type.class, t -> {
							return type.final_().getClass().getSimpleName().substring(4).toLowerCase();
						}).applyIf(Object.class, t -> {
							return aliases.computeIfAbsent(t, t_ -> {
								var c = alias.get();
								alias.update((char) (c + 1));
								return c;
							}).toString();
						}).nonNullResult();
					} finally {
						ins.remove(type);
					}
				else
					return "<recurse>";
			}
		}.toString(type);
	}

	private Type typeOf(Funp n) {
		return (Type) typeByNode.get(n).final_();
	}

	private int getTypeSize(UnNode<Type> n) {
		var result = new Switch<Integer>(n.final_() //
		).applyIf(TypeArray.class, t -> t.apply((elementType, size) -> {
			return getTypeSize(elementType) * size;
		})).applyIf(TypeBoolean.class, t -> {
			return Funp_.booleanSize;
		}).applyIf(TypeByte.class, t -> {
			return 1;
		}).applyIf(TypeIo.class, t -> t.apply(type -> {
			return getTypeSize(type);
		})).applyIf(TypeLambda.class, t -> t.apply((parameterType, returnType) -> {
			return ps + ps;
		})).applyIf(TypeNumber.class, t -> {
			return is;
		}).applyIf(TypeReference.class, t -> t.apply(type -> {
			return ps;
		})).applyIf(TypeStruct.class, t -> t.apply(pairs -> {
			return Read.from(pairs).toInt(Obj_Int.sum(field -> getTypeSize(field.t1)));
		})).result();

		return result != null ? result.intValue() : Funp_.fail("cannot get size of type " + n);
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
			var t = new TypeArray();
			t.elementType = elementType;
			t.size = size;
			return t;
		}

		private <R> R apply(FixieFun2<UnNode<Type>, Integer, R> fun) {
			var ta = finalArray();
			return fun.apply(ta.elementType, ta.size);
		}

		public boolean unify(UnNode<Type> type) {
			var b = getClass() == type.getClass();

			if (b) {
				var x = finalArray();
				var y = ((TypeArray) type).finalArray();
				var ord = x.id < y.id;
				var ta0 = ord ? x : y;
				var ta1 = ord ? y : x;

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
	}

	private static class TypeByte extends Type {
	}

	private static class TypeIo extends Type {
		private UnNode<Type> type;

		private static TypeIo of(UnNode<Type> type) {
			var t = new TypeIo();
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
			var t = new TypeLambda();
			t.parameterType = parameterType;
			t.returnType = returnType;
			return t;
		}

		private <R> R apply(FixieFun2<UnNode<Type>, UnNode<Type>, R> fun) {
			return fun.apply(parameterType, returnType);
		}
	}

	private static class TypeNumber extends Type {
	}

	private static class TypeReference extends Type {
		private UnNode<Type> type;

		private static TypeReference of(UnNode<Type> type) {
			var t = new TypeReference();
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
			var b = getClass() == type.getClass();

			if (b) {
				var x = finalStruct();
				var y = ((TypeStruct) type).finalStruct();
				var ord = x.id < y.id;
				var ts0 = ord ? x : y;
				var ts1 = ord ? y : x;
				var typeByField0 = Read.from2(ts0.pairs).toMap();
				var typeByField1 = Read.from2(ts1.pairs).toMap();

				for (var e : ts1.pairs) {
					var field = e.t0;
					var type0 = typeByField0.get(field);
					var type1 = e.t1;
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
					&& fields().isAll(field -> rethrow(() -> unify.unify(cast(field.get(this)), cast(field.get(type)))));
		}

		private static UnNode<Type> cast(Object object) {
			@SuppressWarnings("unchecked")
			var node = (UnNode<Type>) object;
			return node;
		}
	}

}
