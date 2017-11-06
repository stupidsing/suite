package suite.funp;

import java.util.ArrayList;
import java.util.List;

import suite.adt.Mutable;
import suite.adt.pair.Fixie_.FixieFun0;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Fixie_.FixieFun4;
import suite.adt.pair.Pair;
import suite.assembler.Amd64.OpReg;
import suite.funp.Funp_.Funp;
import suite.node.Atom;
import suite.node.Node;
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

	public static class FunpAsm implements Funp, P3.End {
		public List<Pair<OpReg, Funp>> assigns;
		public List<Node> asm;

		public static FunpAsm of(List<Pair<OpReg, Funp>> assigns, List<Node> asm) {
			FunpAsm f = new FunpAsm();
			f.assigns = assigns;
			f.asm = asm;
			return f;
		}

		public <R> R apply(FixieFun2<List<Pair<OpReg, Funp>>, List<Node>, R> fun) {
			return fun.apply(assigns, asm);
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

	public static class FunpBoolean implements Funp, P3.End {
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

	public static class FunpCheckType implements Funp, P3.End {
		public Funp left;
		public Funp right;
		public Funp expr;

		public static FunpCheckType of(Funp left, Funp right, Funp expr) {
			FunpCheckType f = new FunpCheckType();
			f.left = left;
			f.right = right;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<Funp, Funp, Funp, R> fun) {
			return fun.apply(left, right, expr);
		}
	}

	public static class FunpCoerce implements Funp, P1.End {
		public String coerce;
		public Funp expr;

		public static FunpCoerce of(String coerce, Funp expr) {
			FunpCoerce f = new FunpCoerce();
			f.coerce = coerce;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun2<String, Funp, R> fun) {
			return fun.apply(coerce, expr);
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

	public static class FunpDefineRec implements Funp, P1.End {
		public List<Pair<String, Funp>> pairs;
		public Funp expr;

		public static FunpDefineRec of(List<Pair<String, Funp>> pairs, Funp expr) {
			FunpDefineRec f = new FunpDefineRec();
			f.pairs = pairs;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun2<List<Pair<String, Funp>>, Funp, R> fun) {
			return fun.apply(pairs, expr);
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

	public static class FunpDontCare implements Funp, P3.End {
		public static FunpDontCare of() {
			return new FunpDontCare();
		}

		public <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
		}
	}

	public static class FunpError implements Funp, P3.End {
		public static FunpError of() {
			return new FunpError();
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

	public static class FunpFixed implements Funp, P3.End {
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

	public static class FunpIf implements Funp, P3.End {
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

	public static class FunpIndex implements Funp, P3.End {
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

	public static class FunpIterate implements Funp, P1.End {
		public String var;
		public Funp init;
		public Funp cond;
		public Funp iterate;

		public static FunpIterate of(String var, Funp init, Funp cond, Funp iterate) {
			FunpIterate f = new FunpIterate();
			f.var = var;
			f.init = init;
			f.cond = cond;
			f.iterate = iterate;
			return f;
		}

		public <R> R apply(FixieFun4<String, Funp, Funp, Funp, R> fun) {
			return fun.apply(var, init, cond, iterate);
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

	public static class FunpNumber implements Funp, P3.End {
		public Mutable<Integer> i;

		public static FunpNumber ofNumber(int i) {
			return of(Mutable.of(i));
		}

		public static FunpNumber of(Mutable<Integer> i) {
			FunpNumber f = new FunpNumber();
			f.i = i;
			return f;
		}

		public <R> R apply(FixieFun1<Mutable<Integer>, R> fun) {
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

	public static class FunpRepeat implements Funp, P1.End {
		public int count;
		public Funp expr;

		public static FunpRepeat of(int count, Funp expr) {
			FunpRepeat f = new FunpRepeat();
			f.count = count;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun2<Integer, Funp, R> fun) {
			return fun.apply(count, expr);
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

	public static class FunpTree implements Funp, P3.End {
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

		public <R> R apply(FixieFun3<Operator, Funp, Funp, R> fun) {
			return fun.apply(operator, left, right);
		}
	}

	public static class FunpTree2 implements Funp, P3.End {
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
