package suite.funp;

import suite.adt.Mutable;
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
import suite.immutable.IMap;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.primitive.IntInt_Int;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;

public class P1GenerateLambda {

	public static class Rt {
		private Rt parent;
		private Value var;

		public Rt(Rt parent, Value var) {
			this.parent = parent;
			this.var = var;
		}
	}

	public interface Value {
	}

	public static class Bool implements Value {
		public final boolean b;

		private Bool(boolean b) {
			this.b = b;
		}
	}

	public static class Int implements Value {
		public final int i;

		private Int(int i) {
			this.i = i;
		}
	}

	public static class Vec implements Value {
		public final Value[] values;

		private Vec(Value[] values) {
			this.values = values;
		}
	}

	public interface Thunk extends Fun<Rt, Value>, Value {
	}

	private interface Fun_ extends Fun<Value, Value>, Value {
	}

	public Thunk compile(int fs, IMap<String, Integer> env, Funp n0) {
		return new Compile(fs, env).compile_(n0);
	}

	private class Compile {
		private int fs;
		private IMap<String, Integer> env;

		private Compile(int fs, IMap<String, Integer> env) {
			this.fs = fs;
			this.env = env;
		}

		private Thunk compile_(Funp n0) {
			if (n0 instanceof FunpApply) {
				FunpApply n1 = (FunpApply) n0;
				Thunk lambda = compile_(n1.lambda);
				Thunk value = compile_(n1.value);
				return rt -> ((Fun_) lambda.apply(rt)).apply(value.apply(rt));
			} else if (n0 instanceof FunpArray) {
				FunpArray n1 = (FunpArray) n0;
				Streamlet<Thunk> thunks = Read.from(n1.elements).map(element -> compile_(element));
				return rt -> new Vec(thunks.map(thunk -> thunk.apply(rt)).toArray(Value.class));
			} else if (n0 instanceof FunpBoolean) {
				Bool b = new Bool(((FunpBoolean) n0).b);
				return rt -> b;
			} else if (n0 instanceof FunpDefine) {
				FunpDefine n1 = (FunpDefine) n0;
				return compile_(FunpApply.of(n1.value, FunpLambda.of(n1.var, n1.expr)));
			} else if (n0 instanceof FunpDeref)
				throw new RuntimeException();
			else if (n0 instanceof FunpFixed) {
				FunpLambda n1 = (FunpLambda) n0;
				int fs1 = fs + 1;
				Thunk thunk = compile(fs1, env.put(n1.var, fs1), n1.expr);
				return rt -> {
					Mutable<Fun_> mut = Mutable.nil();
					Fun_ fun = p -> thunk.apply(new Rt(rt, mut.get()));
					mut.set(fun);
					return fun;
				};
			} else if (n0 instanceof FunpIf) {
				FunpIf n1 = (FunpIf) n0;
				Thunk if_ = compile_(n1.if_);
				Thunk then = compile_(n1.then);
				Thunk else_ = compile_(n1.else_);
				return rt -> (b(rt, if_) ? then : else_).apply(rt);
			} else if (n0 instanceof FunpIndex) {
				FunpIndex n1 = (FunpIndex) n0;
				Thunk array = compile_(n1.array);
				Thunk index = compile_(n1.index);
				return rt -> ((Vec) array.apply(rt)).values[i(rt, index)];
			} else if (n0 instanceof FunpLambda) {
				FunpLambda n1 = (FunpLambda) n0;
				int fs1 = fs + 1;
				Thunk thunk = compile(fs1, env.put(n1.var, fs1), n1.expr);
				return rt -> (Fun_) p -> thunk.apply(new Rt(rt, p));
			} else if (n0 instanceof FunpNumber) {
				Int i = new Int(((FunpNumber) n0).i);
				return rt -> i;
			} else if (n0 instanceof FunpPolyType)
				return compile_(((FunpPolyType) n0).expr);
			else if (n0 instanceof FunpReference)
				throw new RuntimeException();
			else if (n0 instanceof FunpTree) {
				FunpTree n1 = (FunpTree) n0;
				Thunk v0 = compile_(n1.left);
				Thunk v1 = compile_(n1.right);
				if (n1.operator == TermOp.BIGAND)
					return rt -> new Bool(b(rt, v0) && b(rt, v1));
				else if (n1.operator == TermOp.BIGOR_)
					return rt -> new Bool(b(rt, v0) || b(rt, v1));
				else {
					IntInt_Int fun = TreeUtil.evaluateOp((TermOp) n1.operator);
					return rt -> new Int(fun.apply(i(rt, v0), i(rt, v1)));
				}
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
	}

	private static int i(Rt rt, Thunk value) {
		return ((Int) value.apply(rt)).i;
	}

	private static boolean b(Rt rt, Thunk value) {
		return ((Bool) value.apply(rt)).b;
	}

}
