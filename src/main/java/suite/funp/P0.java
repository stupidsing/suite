package suite.funp;

import suite.funp.Funp_.Funp;

public class P0 {

	public static class FunpAddress implements Funp {
		public Funp expr;

		public FunpAddress(Funp expr) {
			this.expr = expr;
		}
	}

	public static class FunpApply implements Funp {
		public Funp lambda;
		public Funp value;

		public FunpApply(Funp lambda, Funp value) {
			this.lambda = lambda;
			this.value = value;
		}
	}

	public static class FunpBoolean implements Funp {
		public boolean b;

		public FunpBoolean(boolean b) {
			this.b = b;
		}
	}

	public static class FunpFixed implements Funp {
		public String var;
		public Funp expr;

		public FunpFixed(String var, Funp expr) {
			this.var = var;
			this.expr = expr;
		}
	}

	public static class FunpIf implements Funp {
		public Funp if_;
		public Funp then;
		public Funp else_;

		public FunpIf(Funp if_, Funp then, Funp else_) {
			this.if_ = if_;
			this.then = then;
			this.else_ = else_;
		}
	}

	public static class FunpLambda implements Funp {
		public String var;
		public Funp expr;

		public FunpLambda(String var, Funp expr) {
			this.var = var;
			this.expr = expr;
		}
	}

	public static class FunpNumber implements Funp {
		public int i;

		public FunpNumber(int i) {
			this.i = i;
		}
	}

	public static class FunpPolyType implements Funp {
		public Funp expr;

		public FunpPolyType(Funp expr) {
			this.expr = expr;
		}
	}

	public static class FunpReference implements Funp {
		public Funp expr;

		public FunpReference(Funp expr) {
			this.expr = expr;
		}
	}

	public static class FunpVariable implements Funp {
		public String var;

		public FunpVariable(String var) {
			this.var = var;
		};
	}

}
