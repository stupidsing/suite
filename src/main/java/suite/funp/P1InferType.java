package suite.funp;

import java.util.HashMap;
import java.util.Map;

import suite.fp.Unify;
import suite.fp.Unify.UnNode;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpFixed;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPolyType;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpVariable;
import suite.funp.P1.FunpAllocStack;
import suite.funp.P1.FunpFramePointer;
import suite.funp.P1.FunpInvokeInt;
import suite.funp.P1.FunpMemory;
import suite.funp.P1.FunpRoutine;
import suite.funp.P1.FunpRoutine2;
import suite.funp.P1.FunpSaveFramePointer;
import suite.funp.P1.FunpSaveRegisters;
import suite.immutable.IMap;
import suite.inspect.Inspect;
import suite.node.io.TermOp;
import suite.node.util.Singleton;
import suite.util.AutoObject;
import suite.util.Rethrow;

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

	private static class TypeBoolean extends Type {
	}

	private static class TypeNumber extends Type {
	}

	private static class TypeLambda extends Type {
		private UnNode<Type> parameterType, returnType;

		private TypeLambda(UnNode<Type> parameterType, UnNode<Type> returnType) {
			this.parameterType = parameterType;
			this.returnType = returnType;
		}
	}

	private static class TypeReference extends Type {
		private UnNode<Type> type;
	}

	private Inspect inspect = Singleton.me.inspect;

	private UnNode<Type> typeBoolean = new TypeBoolean();
	private UnNode<Type> typeNumber = new TypeNumber();
	private Map<Funp, UnNode<Type>> typeByNode = new HashMap<>();

	public Funp infer(Funp n0) {
		return infer(n0, unify.newRef());
	}

	public Funp infer(Funp n0, UnNode<Type> t) {
		UnNode<Type> t0 = typeNumber;
		UnNode<Type> t1 = new TypeLambda(typeNumber, t0);
		UnNode<Type> t2 = new TypeLambda(typeNumber, t1);
		IMap<String, UnNode<Type>> env = IMap.<String, UnNode<Type>> empty() //
				.put(TermOp.PLUS__.name, t2) //
				.put(TermOp.MINUS_.name, t2) //
				.put(TermOp.MULT__.name, t2);

		if (unify.unify(t, infer(env, n0)))
			return rewrite(0, IMap.empty(), n0);
		else
			throw new RuntimeException("cannot infer type for " + n0);
	}

	private UnNode<Type> infer(IMap<String, UnNode<Type>> env, Funp n0) {
		UnNode<Type> t = typeByNode.get(n0);
		if (t == null)
			typeByNode.put(n0, t = infer_(env, n0));
		return t;
	}

	private UnNode<Type> infer_(IMap<String, UnNode<Type>> env, Funp n0) {
		if (n0 instanceof FunpApply) {
			FunpApply n1 = (FunpApply) n0;
			TypeLambda tl = cast(TypeLambda.class, infer(env, n1.lambda));
			if (unify.unify(tl.parameterType, infer(env, n1.value)))
				return tl.returnType;
			else
				throw new RuntimeException("cannot infer type for " + n0);
		} else if (n0 instanceof FunpBoolean)
			return typeBoolean;
		else if (n0 instanceof FunpFixed) {
			FunpFixed n1 = (FunpFixed) n0;
			UnNode<Type> t = unify.newRef();
			if (unify.unify(t, infer(env.put(n1.var, t), n1.expr)))
				return t;
			else
				throw new RuntimeException("cannot infer type for " + n0);
		} else if (n0 instanceof FunpIf) {
			FunpIf n1 = (FunpIf) n0;
			UnNode<Type> t;
			if (unify.unify(typeBoolean, infer(env, n1.if_)) && unify.unify(t = infer(env, n1.then), infer(env, n1.else_)))
				return t;
			else
				throw new RuntimeException("cannot infer type for " + n0);
		} else if (n0 instanceof FunpLambda) {
			FunpLambda n1 = (FunpLambda) n0;
			UnNode<Type> tv = unify.newRef();
			return new TypeLambda(tv, infer(env.put(n1.var, tv), n1.expr));
		} else if (n0 instanceof FunpNumber)
			return typeNumber;
		else if (n0 instanceof FunpPolyType)
			return infer(env, ((FunpPolyType) n0).expr).clone_();
		else if (n0 instanceof FunpReference)
			return cast(TypeReference.class, infer(env, ((FunpReference) n0).expr)).type;
		else if (n0 instanceof FunpTree) {
			UnNode<Type> t0 = infer_(env, ((FunpTree) n0).left);
			UnNode<Type> t1 = infer_(env, ((FunpTree) n0).right);
			if (unify.unify(t0, typeNumber) && unify.unify(t1, typeNumber))
				return typeNumber;
			else
				throw new RuntimeException("cannot infer type for " + n0);
		} else if (n0 instanceof FunpVariable)
			return env.get(((FunpVariable) n0).var);
		else
			throw new RuntimeException("cannot infer type for " + n0);
	}

	private Funp rewrite(int scope, IMap<String, Var> env, Funp n0) {
		return inspect.rewrite(Funp.class, n -> rewrite_(scope, env, n), n0);
	}

	private Funp rewrite_(int scope, IMap<String, Var> env, Funp n0) {
		if (n0 instanceof FunpApply) {
			FunpApply n1 = (FunpApply) n0;
			Funp p = n1.value;
			Funp lambda0 = n1.lambda;
			LambdaType lt = lambdaType(lambda0);
			Funp lambda1 = rewrite(scope, env, lambda0);
			FunpAllocStack invoke = FunpAllocStack.of(lt.is, p, FunpInvokeInt.of(lambda1));
			return FunpSaveFramePointer.of(FunpSaveRegisters.of(invoke));
		} else if (n0 instanceof FunpLambda) {
			FunpLambda n1 = (FunpLambda) n0;
			String var = n1.var;
			int scope1 = scope + 1;
			LambdaType lt = lambdaType(n0);
			Funp expr = rewrite(scope1, env.put(var, new Var(scope1, 0, lt.is)), n1.expr);
			if (lt.os == Funp_.pointerSize)
				return FunpRoutine.of(expr);
			else if (lt.os == Funp_.pointerSize * 2)
				return FunpRoutine2.of(expr);
			else
				throw new RuntimeException();
		} else if (n0 instanceof FunpPolyType)
			return rewrite(scope, env, ((FunpPolyType) n0).expr);
		else if (n0 instanceof FunpVariable) {
			Var vd = env.get(((FunpVariable) n0).var);
			int scope1 = vd.scope;
			Funp nfp = new FunpFramePointer();
			while (scope != scope1) {
				nfp = FunpMemory.of(nfp, 0, Funp_.pointerSize);
				scope1--;
			}
			return FunpMemory.of(nfp, vd.start, vd.end);
		} else
			return null;
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
			TypeLambda lambdaType = (TypeLambda) typeByNode.get(lambda);
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

	private int getTypeSize(UnNode<Type> n0) {
		UnNode<Type> n1 = n0.final_();
		if (n1 instanceof TypeBoolean)
			return Funp_.booleanSize;
		else if (n1 instanceof TypeLambda)
			return Funp_.pointerSize + Funp_.pointerSize;
		else if (n1 instanceof TypeNumber)
			return Funp_.integerSize;
		else if (n1 instanceof TypeReference)
			return Funp_.pointerSize;
		else
			throw new RuntimeException("cannot infer type for " + n1);
	}

}
