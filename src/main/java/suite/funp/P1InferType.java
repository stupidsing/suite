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
import suite.funp.P0.FunpVariable;
import suite.funp.P1.FunpAllocStack;
import suite.funp.P1.FunpAssign;
import suite.funp.P1.FunpFramePointer;
import suite.funp.P1.FunpInvoke;
import suite.funp.P1.FunpMemory;
import suite.funp.P1.FunpSaveEbp;
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

	private Inspect inspect = new Inspect();
	private Trail trail = new Trail();
	private Map<Funp, Node> typeByNode = new HashMap<>();

	public Funp infer(Funp n0, Node t) {
		if (bind(t, infer(n0)))
			return rewrite(0, IMap.empty(), n0);
		else
			throw new RuntimeException("cannot infer type for " + n0);
	}

	private Node infer(Funp funp) {
		IMap<String, Node> env = IMap.<String, Node>empty() //
				.put(TermOp.PLUS__.getName(), Suite.substitute(".0 => .1 => .2", ftNumber, ftNumber, ftNumber));

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
			if (!bind(m[0], infer(env, n1.value)))
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
		else if (n0 instanceof FunpVariable)
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
			int size = getTypeSize(typeByNode.get(p));
			Funp lambda = rewrite(scope, env, n1.lambda);
			FunpAllocStack invoke = new FunpAllocStack(size, buffer -> new FunpAssign(buffer, p, new FunpInvoke(lambda)));
			return new FunpSaveEbp(new FunpSaveRegisters(invoke));
		} else if (n0 instanceof FunpLambda) {
			String var = ((FunpLambda) n0).var;
			int scope1 = scope + 1;
			int vs = getTypeSize(defLambda.apply(typeByNode.get(n0))[0]);
			return rewrite(scope1, env.put(var, new Var(scope1, vs)), n0);
		} else if (n0 instanceof FunpPolyType)
			return rewrite(scope, env, ((FunpPolyType) n0).expr);
		else if (n0 instanceof FunpVariable) {
			Var vd = env.get(((FunpVariable) n0).var);
			int scope1 = vd.scope;
			int size = vd.size;
			Funp n1 = new FunpFramePointer();

			while (scope != scope1) {
				n1 = new FunpMemory(n1, 0, Funp_.pointerSize);
				scope1--;
			}

			return new FunpMemory(n1, size + Funp_.pointerSize * 2, size);
		} else
			return null;
	}

	private class Var {
		private int scope;
		private int size;

		public Var(int scope, int size) {
			this.scope = scope;
			this.size = size;
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

	private boolean bind(Node ft0, Node ft1) {
		return Binder.bind(ft0, ft1, trail);
	}

}
