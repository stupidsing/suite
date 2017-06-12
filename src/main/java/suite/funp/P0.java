package suite.funp;

import suite.funp.Funp_.PN0;
import suite.funp.Funp_.PN1;

public class P0 {

	public static class FunpAddress implements PN0, PN1 {
		public final PN0 expr;

		public FunpAddress(PN0 expr) {
			this.expr = expr;
		}
	}

	public static class FunpApply implements PN0, PN1 {
		public final PN0 lambda;
		public final PN0 value;

		public FunpApply(PN0 lambda, PN0 value) {
			this.lambda = lambda;
			this.value = value;
		}
	}

	public static class FunpBoolean implements PN0, PN1 {
		public final boolean b;

		public FunpBoolean(boolean b) {
			this.b = b;
		}
	}

	public static class FunpFixed implements PN0 {
		public final String var;
		public final PN0 expr;

		public FunpFixed(String var, PN0 expr) {
			this.var = var;
			this.expr = expr;
		}
	}

	public static class FunpIf implements PN0, PN1 {
		public final PN0 if_;
		public final PN0 then;
		public final PN0 else_;

		public FunpIf(PN0 if_, PN0 then, PN0 else_) {
			this.if_ = if_;
			this.then = then;
			this.else_ = else_;
		}
	}

	public static class FunpLambda implements PN0 {
		public final String var;
		public final PN0 expr;

		public FunpLambda(String var, PN0 expr) {
			this.var = var;
			this.expr = expr;
		}
	}

	public static class FunpNumber implements PN0, PN1 {
		public final int i;

		public FunpNumber(int i) {
			this.i = i;
		}
	}

	public static class FunpPolyType implements PN0 {
		public final PN0 expr;

		public FunpPolyType(PN0 expr) {
			this.expr = expr;
		}
	}

	public static class FunpReference implements PN0, PN1 {
		public final PN0 expr;

		public FunpReference(PN0 expr) {
			this.expr = expr;
		}
	}

	public static class FunpVariable implements PN0 {
		public final String var;

		public FunpVariable(String var) {
			this.var = var;
		};
	}

}
