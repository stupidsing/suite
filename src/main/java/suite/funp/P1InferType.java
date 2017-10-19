package suite.funp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Fixie_.FixieFun0;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.fp.Unify;
import suite.fp.Unify.UnNode;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpFixed;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPolyType;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpVariable;
import suite.funp.P1.FunpAllocStack;
import suite.funp.P1.FunpData;
import suite.funp.P1.FunpInvokeInt;
import suite.funp.P1.FunpInvokeInt2;
import suite.funp.P1.FunpInvokeIo;
import suite.funp.P1.FunpMemory;
import suite.funp.P1.FunpRoutine;
import suite.funp.P1.FunpRoutine2;
import suite.funp.P1.FunpRoutineIo;
import suite.funp.P1.FunpSaveRegisters;
import suite.immutable.IMap;
import suite.inspect.Inspect;
import suite.node.io.TermOp;
import suite.node.util.Singleton;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.IntIntPair;
import suite.streamlet.Read;
import suite.util.AutoObject;
import suite.util.Rethrow;
import suite.util.String_;

/**
 * Hindley-Milner type inference.
 *
 * @author ywsing
 */
public class P1InferType {

	private static Unify<Type> unify = new Unify<>();

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

	private static class TypeArray extends Type {
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

		public boolean unify(UnNode<Type> type) {
			if (getClass() == type.getClass()) {
				TypeArray other = (TypeArray) type;
				if (unify.unify(elementType, other.elementType)) {
					if (size == -1)
						size = other.size;
					else if (other.size == -1)
						other.size = size;
					return size == other.size;
				} else
					return false;
			} else
				return false;
		}
	}

	private static class TypeBoolean extends Type {
	}

	private static class TypeLambda extends Type {
		private UnNode<Type> parameterType, returnType;

		private static TypeLambda of(UnNode<Type> parameterType, UnNode<Type> returnType) {
			TypeLambda t = new TypeLambda();
			t.parameterType = parameterType;
			t.returnType = returnType;
			return t;
		}
	}

	private static class TypeNumber extends Type {
	}

	private static class TypeReference extends Type {
		@SuppressWarnings("unused")
		private UnNode<Type> type;

		private static TypeReference of(UnNode<Type> type) {
			TypeReference t = new TypeReference();
			t.type = type;
			return t;
		}
	}

	private Inspect inspect = Singleton.me.inspect;

	private UnNode<Type> typeBoolean = new TypeBoolean();
	private UnNode<Type> typeNumber = new TypeNumber();
	private Map<Funp, UnNode<Type>> typeByNode = new HashMap<>();

	public Funp infer(Funp n) {
		return infer(n, unify.newRef());
	}

	public Funp infer(Funp n0, UnNode<Type> t) {
		UnNode<Type> t0 = typeNumber;
		UnNode<Type> t1 = TypeLambda.of(typeNumber, t0);
		UnNode<Type> t2 = TypeLambda.of(typeNumber, t1);
		IMap<String, UnNode<Type>> env = IMap.<String, UnNode<Type>>empty() //
				.put(TermOp.BIGAND.name, t2) //
				.put(TermOp.BIGOR_.name, t2) //
				.put(TermOp.PLUS__.name, t2) //
				.put(TermOp.MINUS_.name, t2) //
				.put(TermOp.MULT__.name, t2) //
				.put(TermOp.DIVIDE.name, t2);

		if (unify.unify(t, new Infer(env).infer(n0)))
			return erase(0, 0, IMap.empty(), n0);
		else
			throw new RuntimeException("cannot infer type for " + n0);
	}

	private class Infer {
		private IMap<String, UnNode<Type>> env;

		private Infer(IMap<String, UnNode<Type>> env) {
			this.env = env;
		}

		private UnNode<Type> infer(Funp n0) {
			UnNode<Type> t = typeByNode.get(n0);
			if (t == null)
				typeByNode.put(n0, t = infer_(n0));
			return t;
		}

		private UnNode<Type> infer_(Funp n0) {
			if (n0 instanceof FunpApply) {
				FunpApply n1 = (FunpApply) n0;
				TypeLambda tl = cast(TypeLambda.class, infer(n1.lambda));
				unify(n0, tl.parameterType, infer(n1.value));
				return tl.returnType;
			} else if (n0 instanceof FunpArray) {
				List<Funp> elements = ((FunpArray) n0).elements;
				UnNode<Type> te = unify.newRef();
				for (Funp element : elements)
					unify(n0, te, infer_(element));
				return TypeArray.of(te, elements.size());
			} else if (n0 instanceof FunpBoolean)
				return typeBoolean;
			else if (n0 instanceof FunpDeref) {
				UnNode<Type> t = unify.newRef();
				unify(n0, TypeReference.of(t), infer(((FunpDeref) n0).pointer));
				return t;
			} else if (n0 instanceof FunpDefine) {
				FunpDefine n1 = (FunpDefine) n0;
				return new Infer(env.put(n1.var, infer(n1.value))).infer(n1.expr);
			} else if (n0 instanceof FunpFixed) {
				FunpFixed n1 = (FunpFixed) n0;
				UnNode<Type> t = unify.newRef();
				unify(n0, t, new Infer(env.put(n1.var, t)).infer(n1.expr));
				return t;
			} else if (n0 instanceof FunpIf) {
				FunpIf n1 = (FunpIf) n0;
				UnNode<Type> t;
				unify(n0, typeBoolean, infer(n1.if_));
				unify(n0, t = infer(n1.then), infer(n1.else_));
				return t;
			} else if (n0 instanceof FunpIndex) {
				FunpIndex n1 = (FunpIndex) n0;
				UnNode<Type> t = unify.newRef();
				unify(n0, TypeArray.of(t), infer(n1.array));
				return t;
			} else if (n0 instanceof FunpLambda) {
				FunpLambda n1 = (FunpLambda) n0;
				UnNode<Type> tv = unify.newRef();
				return TypeLambda.of(tv, new Infer(env.put(n1.var, tv)).infer(n1.expr));
			} else if (n0 instanceof FunpNumber)
				return typeNumber;
			else if (n0 instanceof FunpPolyType)
				return unify.clone(infer(((FunpPolyType) n0).expr));
			else if (n0 instanceof FunpReference)
				return TypeReference.of(infer(((FunpReference) n0).expr));
			else if (n0 instanceof FunpTree) {
				FunpTree n1 = (FunpTree) n0;
				UnNode<Type> t0 = infer(n1.left);
				UnNode<Type> t1 = infer(n1.right);
				unify(n0, t0, typeNumber);
				unify(n0, t1, typeNumber);
				return typeNumber;
			} else if (n0 instanceof FunpVariable)
				return env.get(((FunpVariable) n0).var);
			else
				throw new RuntimeException("cannot infer type for " + n0);
		}
	}

	private void unify(Funp n0, UnNode<Type> type0, UnNode<Type> type1) {
		if (!unify.unify(type0, type1))
			throw new RuntimeException("cannot infer type for " + n0);
	}

	private Funp erase(int scope, int fs, IMap<String, Var> env, Funp n) {
		return new Erase(scope, fs, env).erase(n);
	}

	private class Erase {
		private int scope;
		private int fs;
		private IMap<String, Var> env;

		private Erase(int scope, int fs, IMap<String, Var> env) {
			this.scope = scope;
			this.fs = fs;
			this.env = env;
		}

		private Funp erase(Funp n) {
			return inspect.rewrite(Funp.class, this::erase_, n);
		}

		private Funp erase_(Funp n0) {
			if (n0 instanceof FunpApply) {
				FunpApply n1 = (FunpApply) n0;
				Funp value = n1.value;
				Funp lambda = n1.lambda;

				if (Boolean.TRUE || !(lambda instanceof FunpLambda)) {
					LambdaType lt = lambdaType(lambda);
					Funp lambda1 = erase(lambda);
					Funp invoke;
					if (lt.os == Funp_.pointerSize)
						invoke = allocStack(value, FunpInvokeInt.of(lambda1));
					else if (lt.os == Funp_.pointerSize * 2)
						invoke = allocStack(value, FunpInvokeInt2.of(lambda1));
					else
						invoke = FunpAllocStack.of(lt.os, null, allocStack(value, FunpInvokeIo.of(lambda1)));
					return FunpSaveRegisters.of(invoke);
				} else {
					FunpLambda lambda1 = (FunpLambda) lambda;
					return erase(FunpDefine.of(lambda1.var, value, lambda1.expr));
				}
			} else if (n0 instanceof FunpArray) {
				UnNode<Type> elementType = ((TypeArray) typeOf(n0)).elementType.final_();
				List<Funp> elements = Read //
						.from(((FunpArray) n0).elements) //
						.map(this::erase) //
						.toList();
				int elementSize = getTypeSize(elementType);
				IntIntPair[] offsets = Ints_ //
						.range(0, elements.size()) //
						.map(i -> IntIntPair.of(i * elementSize, (i + 1) * elementSize)) //
						.toArray(IntIntPair.class);
				return FunpData.of(elements, offsets);
			} else if (n0 instanceof FunpDefine) {
				FunpDefine n1 = (FunpDefine) n0;
				String var = n1.var;
				Funp value = n1.value;
				Funp expr = n1.expr;

				if (Boolean.TRUE) {
					int fs1 = fs - getTypeSize(typeOf(value));
					Erase erase1 = new Erase(scope, fs1, env.put(var, new Var(scope, fs1, fs)));
					return allocStack(value, erase1.erase(expr));
				} else
					return erase(new Expand(var, n1.value).expand(expr));
			} else if (n0 instanceof FunpDeref) {
				FunpDeref n1 = (FunpDeref) n0;
				return FunpMemory.of(erase(n1.pointer), 0, getTypeSize(typeOf(n1)));
			} else if (n0 instanceof FunpIndex) {
				FunpIndex n1 = (FunpIndex) n0;
				Funp array = n1.array;
				int size = getTypeSize(((TypeArray) typeOf(array)).elementType);
				Funp address0 = getAddress(erase(array));
				FunpTree inc = FunpTree.of(TermOp.MULT__, erase(n1.index), FunpNumber.of(size));
				Funp address1 = FunpTree.of(TermOp.PLUS__, address0, inc);
				return FunpMemory.of(address1, 0, size);
			} else if (n0 instanceof FunpLambda) {
				FunpLambda n1 = (FunpLambda) n0;
				int b = Funp_.pointerSize * 2; // return address and EBP
				String var = n1.var;
				int scope1 = scope + 1;
				LambdaType lt = lambdaType(n0);
				Erase erase1 = new Erase(scope1, 0, env.put(var, new Var(scope1, b, b + lt.is)));
				Funp expr = erase1.erase(n1.expr);
				if (lt.os == Funp_.pointerSize)
					return FunpRoutine.of(expr);
				else if (lt.os == Funp_.pointerSize * 2)
					return FunpRoutine2.of(expr);
				else
					return FunpRoutineIo.of(expr, lt.is, lt.os);
			} else if (n0 instanceof FunpPolyType) {
				Funp expr = ((FunpPolyType) n0).expr;
				return erase(expr);
			} else if (n0 instanceof FunpReference)
				return getAddress(erase(((FunpReference) n0).expr));
			else if (n0 instanceof FunpVariable) {
				Var vd = env.get(((FunpVariable) n0).var);
				return getVariable(vd);
			} else
				return null;
		}

		private Funp allocStack(Funp p, Funp expr) {
			UnNode<Type> t = typeOf(p);
			return FunpAllocStack.of(getTypeSize(t), erase(p), expr);
		}

		private Funp getAddress(Funp n0) {
			if (n0 instanceof FunpMemory) {
				FunpMemory n1 = (FunpMemory) n0;
				return FunpTree.of(TermOp.PLUS__, n1.pointer, FunpNumber.of(n1.start));
			} else if (n0 instanceof FunpVariable) {
				Var vd = env.get(((FunpVariable) n0).var);
				return getAddress(getVariable(vd));
			} else
				throw new RuntimeException();
		}

		private Funp getVariable(Var vd) {
			Funp nfp = Funp_.framePointer;
			for (int i = scope; i < vd.scope; i++)
				nfp = FunpMemory.of(nfp, 0, Funp_.pointerSize);
			return FunpMemory.of(nfp, vd.start, vd.end);
		}
	}

	private class Expand {
		private String var;
		private Funp value;

		private Expand(String var, Funp value) {
			this.var = var;
			this.value = value;
		}

		private Funp expand(Funp n) {
			return inspect.rewrite(Funp.class, this::expand_, n);
		}

		private Funp expand_(Funp n0) {
			if (n0 instanceof FunpDefine) // variable re-defined
				return String_.equals(((FunpDefine) n0).var, var) ? n0 : null;
			else if (n0 instanceof FunpVariable)
				return String_.equals(((FunpVariable) n0).var, var) ? value : n0;
			else
				return null;
		}
	}

	private class Var {
		private int scope;
		private int start;
		private int end;

		public Var(int scope, int start, int end) {
			this.scope = scope;
			this.start = start;
			this.end = end;
		}
	}

	private LambdaType lambdaType(Funp lambda) {
		LambdaType lt = new LambdaType(lambda);
		if (lt.os <= Funp_.integerSize)
			return lt;
		else
			throw new RuntimeException();

	}

	private class LambdaType {
		private int is, os;

		private LambdaType(Funp lambda) {
			TypeLambda lambdaType = (TypeLambda) typeOf(lambda);
			is = getTypeSize(lambdaType.parameterType);
			os = getTypeSize(lambdaType.returnType);
		}
	}

	private <Type1 extends Type> Type1 cast(Class<Type1> clazz, UnNode<Type> type) {
		if (type.getClass() == clazz) {
			@SuppressWarnings("unchecked")
			Type1 t1 = (Type1) type;
			return t1;
		} else
			return null;
	}

	private UnNode<Type> typeOf(Funp n) {
		return typeByNode.get(n);
	}

	private int getTypeSize(UnNode<Type> n) {
		return new SwitchType<Integer>(n.final_()) //
				.ifTypeArray((elementType, size) -> getTypeSize(elementType) * size) //
				.ifTypeBoolean(() -> Funp_.booleanSize) //
				.ifTypeLambda((parameterType, returnType) -> Funp_.pointerSize + Funp_.pointerSize) //
				.ifTypeNumber(() -> Funp_.integerSize) //
				.ifTypeReference((type) -> Funp_.pointerSize) //
				.result();
	}

	private class SwitchType<T> {
		private UnNode<Type> in;
		private T result = null;

		private SwitchType(UnNode<Type> in) {
			this.in = in;
		}

		private SwitchType<T> ifTypeArray(FixieFun2<UnNode<Type>, Integer, T> fun) {
			if (in instanceof TypeArray) {
				TypeArray t = (TypeArray) in;
				result = fun.apply(t.elementType, t.size);
			}
			return this;
		}

		private SwitchType<T> ifTypeBoolean(FixieFun0<T> fun) {
			if (in instanceof TypeBoolean)
				result = fun.apply();
			return this;
		}

		private SwitchType<T> ifTypeLambda(FixieFun2<UnNode<Type>, UnNode<Type>, T> fun) {
			if (in instanceof TypeLambda) {
				TypeLambda t = (TypeLambda) in;
				result = fun.apply(t.parameterType, t.returnType);
			}
			return this;
		}

		private SwitchType<T> ifTypeNumber(FixieFun0<T> fun) {
			if (in instanceof TypeNumber)
				result = fun.apply();
			return this;
		}

		private SwitchType<T> ifTypeReference(FixieFun1<UnNode<Type>, T> fun) {
			if (in instanceof TypeReference) {
				TypeReference t = (TypeReference) in;
				result = fun.apply(t.type);
			}
			return this;
		}

		private T result() {
			if (result != null)
				return result;
			else
				throw new RuntimeException("cannot handle " + in);
		}
	}

}
