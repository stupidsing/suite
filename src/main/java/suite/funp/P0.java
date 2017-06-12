package suite.funp;

import suite.funp.Funp_.Funp;

public class P0 {

	public class FunpAddress extends Funp {
		public final Funp expr;

		public FunpAddress(Funp expr) {
			this.expr = expr;
		}
	}

	public class FunpApply extends Funp {
		public final Funp lambda;
		public final Funp value;

		public FunpApply(Funp lambda, Funp value) {
			this.lambda = lambda;
			this.value = value;
		}
	}

	public class FunpBoolean extends Funp {
		public final boolean b;

		public FunpBoolean(boolean b) {
			this.b = b;
		}
	}

	public class FunpFixed extends Funp {
		public final String var;
		public final Funp expr;

		public FunpFixed(String var, Funp expr) {
			this.var = var;
			this.expr = expr;
		}
	}

	public class FunpIf extends Funp {
		public final Funp if_;
		public final Funp then;
		public final Funp else_;

		public FunpIf(Funp if_, Funp then, Funp else_) {
			this.if_ = if_;
			this.then = then;
			this.else_ = else_;
		}
	}

	public class FunpLambda extends Funp {
		public final String var;
		public final Funp expr;

		public FunpLambda(String var, Funp expr) {
			this.var = var;
			this.expr = expr;
		}
	}

	public class FunpNumber extends Funp {
		public final int i;

		public FunpNumber(int i) {
			this.i = i;
		}
	}

	public class FunpPolyType extends Funp {
		public final Funp expr;

		public FunpPolyType(Funp expr) {
			this.expr = expr;
		}
	}

	public class FunpReference extends Funp {
		public final Funp expr;

		public FunpReference(Funp expr) {
			this.expr = expr;
		}
	}

	public class FunpVariable extends Funp {
		public final String var;

		public FunpVariable(String var) {
			this.var = var;
		};
	}

}
