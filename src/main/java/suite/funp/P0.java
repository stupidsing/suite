package suite.funp;

import java.util.List;

import suite.funp.Funp_.Funp;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;

public class P0 {

	public interface End {
	}

	public static class FunpApply implements Funp, P1.End {
		public Funp value;
		public Funp lambda;

		public static FunpApply of(Funp value, Funp lambda) {
			FunpApply f = new FunpApply();
			f.value = value;
			f.lambda = lambda;
			return f;
		}
	}

	public static class FunpArray implements Funp, P1.End {
		public List<Funp> elements;

		public static FunpArray of(List<Funp> elements) {
			FunpArray f = new FunpArray();
			f.elements = elements;
			return f;
		}
	}

	public static class FunpBoolean implements Funp, P2.End {
		public boolean b;

		public static FunpBoolean of(boolean b) {
			FunpBoolean f = new FunpBoolean();
			f.b = b;
			return f;
		}
	}

	public static class FunpDefine implements Funp, P1.End {
		public String var;
		public Funp value;
		public Funp expr;

		public static FunpDefine of(String var, Funp value, Funp expr) {
			FunpDefine f = new FunpDefine();
			f.var = var;
			f.value = value;
			f.expr = expr;
			return f;
		}
	}

	public static class FunpDeref implements Funp, P1.End {
		public Funp pointer;

		public static FunpDeref of(Funp pointer) {
			FunpDeref f = new FunpDeref();
			f.pointer = pointer;
			return f;
		}
	}

	public static class FunpFixed implements Funp, P2.End {
		public String var;
		public Funp expr;

		public static FunpFixed of(String var, Funp expr) {
			FunpFixed f = new FunpFixed();
			f.var = var;
			f.expr = expr;
			return f;
		}
	}

	public static class FunpIf implements Funp, P2.End {
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

	public static class FunpIndex implements Funp, P2.End {
		public Funp array;
		public Funp index;

		public static FunpIndex of(Funp array, Funp index) {
			FunpIndex f = new FunpIndex();
			f.array = array;
			f.index = index;
			return f;
		}
	}

	public static class FunpLambda implements Funp, P1.End {
		public String var;
		public Funp expr;

		public static FunpLambda of(String var, Funp expr) {
			FunpLambda f = new FunpLambda();
			f.var = var;
			f.expr = expr;
			return f;
		}
	}

	public static class FunpNumber implements Funp, P2.End {
		public int i;

		public static FunpNumber of(int i) {
			FunpNumber f = new FunpNumber();
			f.i = i;
			return f;
		}
	}

	public static class FunpPolyType implements Funp, P1.End {
		public Funp expr;

		public static FunpPolyType of(Funp expr) {
			FunpPolyType f = new FunpPolyType();
			f.expr = expr;
			return f;
		}
	}

	public static class FunpReference implements Funp, P1.End {
		public Funp expr;

		public static FunpReference of(Funp pointer) {
			FunpReference f = new FunpReference();
			f.expr = pointer;
			return f;
		}
	}

	public static class FunpTree implements Funp, P2.End {
		public Operator operator;
		public Funp left;
		public Funp right;

		public static FunpTree of(Operator operator, Funp left, Funp right) {
			FunpTree f = new FunpTree();
			f.operator = operator;
			f.left = left;
			f.right = right;
			return f;
		}

		public Funp getFirst() {
			return operator.getAssoc() == Assoc.RIGHT ? right : left;
		}

		public Funp getSecond() {
			return operator.getAssoc() == Assoc.RIGHT ? left : right;
		}
	}

	public static class FunpVariable implements Funp, P1.End {
		public String var;

		public static FunpVariable of(String var) {
			FunpVariable f = new FunpVariable();
			f.var = var;
			return f;
		};
	}

}
