package suite.funp;

import java.util.HashMap;
import java.util.Map;

import suite.BindArrayUtil.Match;
import suite.Suite;
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
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Cloner;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.TermOp;
import suite.node.util.Singleton;

/**
 * Hindley-Milner type inference.
 *
 * @author ywsing
 */
public class P1InferType {

	private Atom ftBoolean = Atom.of("BOOLEAN");
	private Atom ftNumber = Atom.of("NUMBER");
	private Match defLambda = Suite.match("LAMBDA .0 .1");
	private Match defReference = Suite.match("REF .0");

	private Inspect inspect = Singleton.me.inspect;
	private Trail trail = new Trail();
	private Map<Funp, Node> typeByNode = new HashMap<>();

	public Funp infer(Funp n0, Node t) {
		if (bind(t, infer(n0)))
			return rewrite(0, IMap.empty(), n0);
		else
			throw new RuntimeException("cannot infer type for " + n0);
	}

	private Node infer(Funp funp) {
		Node t0 = ftNumber;
		Node t1 = defLambda.substitute(ftNumber, t0);
		Node t2 = defLambda.substitute(ftNumber, t1);
		IMap<String, Node> env = IMap.<String, Node> empty() //
				.put(TermOp.PLUS__.name, t2) //
				.put(TermOp.MINUS_.name, t2) //
				.put(TermOp.MULT__.name, t2);
		return infer(env, funp);
	}

	private Node infer(IMap<String, Node> env, Funp n0) {
		Node t = typeByNode.get(n0);
		if (t == null)
			typeByNode.put(n0, t = infer_(env, n0));
		return t;
	}

	private Node infer_(IMap<String, Node> env, Funp n0) {
		if (n0 instanceof FunpApply) {
			FunpApply n1 = (FunpApply) n0;
			Node[] m = defLambda.apply(infer(env, n1.lambda));
			if (bind(m[0], infer(env, n1.value)))
				return m[1];
			else
				throw new RuntimeException("cannot infer type for " + n0);
		} else if (n0 instanceof FunpBoolean)
			return ftBoolean;
		else if (n0 instanceof FunpFixed) {
			FunpFixed n1 = (FunpFixed) n0;
			Node t = new Reference();
			if (bind(t, infer(env.put(n1.var, t), n1.expr)))
				return t;
			else
				throw new RuntimeException("cannot infer type for " + n0);
		} else if (n0 instanceof FunpIf) {
			FunpIf n1 = (FunpIf) n0;
			Node t;
			if (bind(ftBoolean, infer(env, n1.if_)) && bind(t = infer(env, n1.then), infer(env, n1.else_)))
				return t;
			else
				throw new RuntimeException("cannot infer type for " + n0);
		} else if (n0 instanceof FunpLambda) {
			FunpLambda n1 = (FunpLambda) n0;
			Node tv = new Reference();
			return defLambda.substitute(tv, infer(env.put(n1.var, tv), n1.expr));
		} else if (n0 instanceof FunpNumber)
			return ftNumber;
		else if (n0 instanceof FunpPolyType)
			return new Cloner().clone(infer(env, ((FunpPolyType) n0).expr));
		else if (n0 instanceof FunpReference)
			return defReference.apply(infer(((FunpReference) n0).expr))[0];
		else if (n0 instanceof FunpTree) {
			Node t0 = infer_(env, ((FunpTree) n0).left);
			Node t1 = infer_(env, ((FunpTree) n0).right);
			if (bind(t0, ftNumber) && bind(t1, ftNumber))
				return ftNumber;
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
			Node[] types = defLambda.apply(typeByNode.get(lambda));
			is = getTypeSize(types[0]);
			os = getTypeSize(types[1]);
		}
	}

	private int getTypeSize(Node n0) {
		if (defLambda.apply(n0) != null)
			return Funp_.pointerSize + Funp_.pointerSize;
		else if (defReference.apply(n0) != null)
			return Funp_.pointerSize;
		else if (bind(ftBoolean, n0))
			return Funp_.booleanSize;
		else if (bind(ftNumber, n0))
			return Funp_.integerSize;
		else
			throw new RuntimeException("cannot infer type for " + n0);
	}

	private boolean bind(Node t0, Node t1) {
		return Binder.bind(t0, t1, trail);
	}

}
