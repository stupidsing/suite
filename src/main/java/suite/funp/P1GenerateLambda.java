package suite.funp;

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
import suite.immutable.IMap;
import suite.node.io.TermOp;
import suite.util.FunUtil.Fun;

public class P1GenerateLambda {

	public static class Runtime {
		private Runtime parent;
		private Object var;
	}

	public interface Thunk extends Fun<Runtime, Object> {
	}

	private interface Fun_ extends Fun<Object, Object> {
	}

	public Thunk compile(int fs, IMap<String, Integer> env, Funp n0) {
		if (n0 instanceof FunpApply) {
			FunpApply n1 = (FunpApply) n0;
			Thunk lambda = compile(fs, env, n1.lambda);
			Thunk value = compile(fs, env, n1.value);
			return rt -> ((Fun_) lambda.apply(rt)).apply(value.apply(rt));
		} else if (n0 instanceof FunpBoolean) {
			Object b = ((FunpBoolean) n0).b;
			return rt -> b;
		} else if (n0 instanceof FunpFixed)
			throw new RuntimeException();
		else if (n0 instanceof FunpIf) {
			FunpIf n1 = (FunpIf) n0;
			Thunk if_ = compile(fs, env, n1.if_);
			Thunk then = compile(fs, env, n1.then);
			Thunk else_ = compile(fs, env, n1.else_);
			return rt -> (isTrue(rt, if_) ? then : else_).apply(rt);
		} else if (n0 instanceof FunpLambda) {
			FunpLambda n1 = (FunpLambda) n0;
			int fs1 = fs + 1;
			Thunk thunk = compile(fs1, env.put(n1.var, fs1), n1.expr);
			return rt -> (Fun_) p -> {
				Runtime rt1 = new Runtime();
				rt1.parent = rt;
				rt1.var = p;
				return thunk.apply(rt1);
			};
		} else if (n0 instanceof FunpNumber) {
			Object i = ((FunpNumber) n0).i;
			return rt -> i;
		} else if (n0 instanceof FunpPolyType)
			return compile(fs, env, ((FunpPolyType) n0).expr);
		else if (n0 instanceof FunpReference)
			throw new RuntimeException();
		else if (n0 instanceof FunpTree) {
			FunpTree n1 = (FunpTree) n0;
			Thunk v0 = compile(fs, env, n1.left);
			Thunk v1 = compile(fs, env, n1.right);
			if (n1.operator == TermOp.BIGAND)
				return rt -> isTrue(rt, v0) && isTrue(rt, v1);
			else if (n1.operator == TermOp.BIGOR_)
				return rt -> isTrue(rt, v0) || isTrue(rt, v1);
			else if (n1.operator == TermOp.PLUS__)
				return rt -> i(rt, v0) + i(rt, v1);
			else if (n1.operator == TermOp.MINUS_)
				return rt -> i(rt, v0) - i(rt, v1);
			else if (n1.operator == TermOp.MULT__)
				return rt -> i(rt, v0) * i(rt, v1);
			else
				throw new RuntimeException();
		} else if (n0 instanceof FunpVariable) {
			int fd = fs - env.get(((FunpVariable) n0).var);
			return rt -> {
				for (int i = 0; i < fd; i++)
					rt = rt.parent;
				return rt.var;
			};
		} else
			throw new RuntimeException("cannot generate lambda for " + n0);
	}

	private static int i(Runtime rt, Thunk value) {
		return (Integer) value.apply(rt);
	}

	private static boolean isTrue(Runtime rt, Thunk value) {
		return value.apply(rt) == Boolean.TRUE;
	}

}
