package suite.funp;

import java.util.ArrayList;
import java.util.List;

import suite.adt.pair.Fixie_.FixieFun0;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Pair;
import suite.funp.Funp_.Funp;
import suite.node.Atom;
import suite.node.io.Operator;

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

		public <R> R apply(FixieFun2<Funp, Funp, R> fun) {
			return fun.apply(value, lambda);
		}
	}

	public static class FunpArray implements Funp, P1.End {
		public List<Funp> elements;

		public static FunpArray of(List<Funp> elements) {
			FunpArray f = new FunpArray();
			f.elements = elements;
			return f;
		}

		public <R> R apply(FixieFun1<List<Funp>, R> fun) {
			return fun.apply(elements);
		}
	}

	public static class FunpAssignReference implements Funp, P1.End {
		public Funp reference;
		public Funp value;
		public Funp expr;

		public static FunpAssignReference of(Funp reference, Funp value, Funp expr) {
			FunpAssignReference f = new FunpAssignReference();
			f.reference = reference;
			f.value = value;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<Funp, Funp, Funp, R> fun) {
			return fun.apply(reference, value, expr);
		}
	}

	public static class FunpBoolean implements Funp, P2.End {
		public boolean b;

		public static FunpBoolean of(boolean b) {
			FunpBoolean f = new FunpBoolean();
			f.b = b;
			return f;
		}

		public <R> R apply(FixieFun1<Boolean, R> fun) {
			return fun.apply(b);
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

		public <R> R apply(FixieFun3<String, Funp, Funp, R> fun) {
			return fun.apply(var, value, expr);
		}
	}

	public static class FunpDeref implements Funp, P1.End {
		public Funp pointer;

		public static FunpDeref of(Funp pointer) {
			FunpDeref f = new FunpDeref();
			f.pointer = pointer;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(pointer);
		}
	}

	public static class FunpDontCare implements Funp, P2.End {
		public static FunpDontCare of() {
			return new FunpDontCare();
		}

		public <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
		}
	}

	public static class FunpField implements Funp, P1.End {
		public Funp reference;
		public String field;

		public static FunpField of(Funp reference, String field) {
			FunpField f = new FunpField();
			f.reference = reference;
			f.field = field;
			return f;
		}

		public <R> R apply(FixieFun2<Funp, String, R> fun) {
			return fun.apply(reference, field);
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

		public <R> R apply(FixieFun2<String, Funp, R> fun) {
			return fun.apply(var, expr);
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

		public <R> R apply(FixieFun3<Funp, Funp, Funp, R> fun) {
			return fun.apply(if_, then, else_);
		}
	}

	public static class FunpIndex implements Funp, P2.End {
		public Funp reference;
		public Funp index;

		public static FunpIndex of(Funp reference, Funp index) {
			FunpIndex f = new FunpIndex();
			f.reference = reference;
			f.index = index;
			return f;
		}

		public <R> R apply(FixieFun2<Funp, Funp, R> fun) {
			return fun.apply(reference, index);
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

		public <R> R apply(FixieFun2<String, Funp, R> fun) {
			return fun.apply(var, expr);
		}
	}

	public static class FunpNumber implements Funp, P2.End {
		public int i;

		public static FunpNumber of(int i) {
			FunpNumber f = new FunpNumber();
			f.i = i;
			return f;
		}

		public <R> R apply(FixieFun1<Integer, R> fun) {
			return fun.apply(i);
		}
	}

	public static class FunpPolyType implements Funp, P1.End {
		public Funp expr;

		public static FunpPolyType of(Funp expr) {
			FunpPolyType f = new FunpPolyType();
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(expr);
		}
	}

	public static class FunpReference implements Funp, P1.End {
		public Funp expr;

		public static FunpReference of(Funp pointer) {
			FunpReference f = new FunpReference();
			f.expr = pointer;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(expr);
		}
	}

	public static class FunpStruct implements Funp, P1.End {
		public List<Pair<String, Funp>> pairs;

		public static FunpStruct of(List<Pair<String, Funp>> pairs) {
			FunpStruct f = new FunpStruct();
			f.pairs = pairs;
			return f;
		}

		public <R> R apply(FixieFun1<List<Pair<String, Funp>>, R> fun) {
			return fun.apply(pairs);
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

		public static List<Funp> unfold(Funp n, Operator op) {
			List<Funp> list = new ArrayList<>();
			FunpTree tree;
			while (n instanceof FunpTree && (tree = (FunpTree) n).operator == op) {
				list.add(tree.left);
				n = tree.right;
			}
			list.add(n);
			return list;
		}

		public <R> R apply(FixieFun3<Operator, Funp, Funp, R> fun) {
			return fun.apply(operator, left, right);
		}
	}

	public static class FunpTree2 implements Funp, P2.End {
		public Atom operator;
		public Funp left;
		public Funp right;

		public static FunpTree2 of(Atom operator, Funp left, Funp right) {
			FunpTree2 f = new FunpTree2();
			f.operator = operator;
			f.left = left;
			f.right = right;
			return f;
		}

		public static List<Funp> unfold(Funp n, Atom op) {
			List<Funp> list = new ArrayList<>();
			FunpTree2 tree;
			while (n instanceof FunpTree && (tree = (FunpTree2) n).operator == op) {
				list.add(tree.left);
				n = tree.right;
			}
			list.add(n);
			return list;
		}

		public <R> R apply(FixieFun3<Atom, Funp, Funp, R> fun) {
			return fun.apply(operator, left, right);
		}
	}

	public static class FunpVerifyType implements Funp, P2.End {
		public Funp left;
		public Funp right;
		public Funp expr;

		public static FunpVerifyType of(Funp left, Funp right, Funp expr) {
			FunpVerifyType f = new FunpVerifyType();
			f.left = left;
			f.right = right;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<Funp, Funp, Funp, R> fun) {
			return fun.apply(left, right, expr);
		}
	}

	public static class FunpVariable implements Funp, P1.End {
		public String var;

		public static FunpVariable of(String var) {
			FunpVariable f = new FunpVariable();
			f.var = var;
			return f;
		};

		public <R> R apply(FixieFun1<String, R> fun) {
			return fun.apply(var);
		}
	}

	public static class FunpVariableNew implements Funp, P1.End {
		public String var;

		public static FunpVariableNew of(String var) {
			FunpVariableNew f = new FunpVariableNew();
			f.var = var;
			return f;
		};

		public <R> R apply(FixieFun1<String, R> fun) {
			return fun.apply(var);
		}
	}

}
