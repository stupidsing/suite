package suite.funp;

import suite.funp.Funp_.Funp;

public class P0 {

	public static class FunpApply implements Funp {
		public Funp lambda;
		public Funp value;

		public static FunpApply of(Funp lambda, Funp value) {
			FunpApply f = new FunpApply();
			f.lambda = lambda;
			f.value = value;
			return f;
		}
	}

	public static class FunpBoolean implements Funp {
		public boolean b;

		public static FunpBoolean of(boolean b) {
			FunpBoolean f = new FunpBoolean();
			f.b = b;
			return f;
		}
	}

	public static class FunpFixed implements Funp {
		public String var;
		public Funp expr;

		public static FunpFixed of(String var, Funp expr) {
			FunpFixed f = new FunpFixed();
			f.var = var;
			f.expr = expr;
			return f;
		}
	}

	public static class FunpIf implements Funp {
		public Funp if_;
		public Funp then;
		public Funp else_;

		public static FunpIf of(Funp if_, Funp then, Funp else_) {
			FunpIf f = new FunpIf();
			f.if_ = if_;
			f.then = then;
			f.else_ = else_;
			return f;
		}
	}

	public static class FunpLambda implements Funp {
		public String var;
		public Funp expr;

		public static FunpLambda of(String var, Funp expr) {
			FunpLambda f = new FunpLambda();
			f.var = var;
			f.expr = expr;
			return f;
		}
	}

	public static class FunpNumber implements Funp {
		public int i;

		public static FunpNumber of(int i) {
			FunpNumber f = new FunpNumber();
			f.i = i;
			return f;
		}
	}

	public static class FunpPolyType implements Funp {
		public Funp expr;

		public static FunpPolyType of(Funp expr) {
			FunpPolyType f = new FunpPolyType();
			f.expr = expr;
			return f;
		}
	}

	public static class FunpReference implements Funp {
		public Funp expr;

		public static FunpReference of(Funp expr) {
			FunpReference f = new FunpReference();
			f.expr = expr;
			return f;
		}
	}

	public static class FunpVariable implements Funp {
		public String var;

		public static FunpVariable of(String var) {
			FunpVariable f = new FunpVariable();
			f.var = var;
			return f;
		};
	}

}
