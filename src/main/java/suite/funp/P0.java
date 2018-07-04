package suite.funp;

import java.util.ArrayList;
import java.util.List;

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
import suite.primitive.IntMutable;

public class P0 {

	public interface End {
	}

	public static class FunpApply implements Funp, P2.End {
		public Funp value;
		public Funp lambda;

		public static FunpApply of(Funp value, Funp lambda) {
			var f = new FunpApply();
			f.value = value;
			f.lambda = lambda;
			return f;
		}

		public <R> R apply(FixieFun2<Funp, Funp, R> fun) {
			return fun.apply(value, lambda);
		}
	}

	public static class FunpArray implements Funp, P2.End {
		public List<Funp> elements;

		public static FunpArray of(List<Funp> elements) {
			var f = new FunpArray();
			f.elements = elements;
			return f;
		}

		public <R> R apply(FixieFun1<List<Funp>, R> fun) {
			return fun.apply(elements);
		}
	}

	public static class FunpBoolean implements Funp, P4.End {
		public boolean b;

		public static FunpBoolean of(boolean b) {
			var f = new FunpBoolean();
			f.b = b;
			return f;
		}

		public <R> R apply(FixieFun1<Boolean, R> fun) {
			return fun.apply(b);
		}
	}

	public static class FunpCheckType implements Funp, P4.End {
		public Funp left;
		public Funp right;
		public Funp expr;

		public static FunpCheckType of(Funp left, Funp right, Funp expr) {
			var f = new FunpCheckType();
			f.left = left;
			f.right = right;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<Funp, Funp, Funp, R> fun) {
			return fun.apply(left, right, expr);
		}
	}

	public static class FunpCoerce implements Funp, P2.End {
		public enum Coerce {
			BYTE, NUMBER, POINTER,
		};

		public Coerce coerce;
		public Funp expr;

		public static FunpCoerce of(Coerce coerce, Funp expr) {
			var f = new FunpCoerce();
			f.coerce = coerce;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun2<Coerce, Funp, R> fun) {
			return fun.apply(coerce, expr);
		}
	}

	public static class FunpDefine implements Funp, P2.End {
		public boolean isPolyType;
		public String var;
		public Funp value;
		public Funp expr;

		public static FunpDefine of(boolean isPolyType, String var, Funp value, Funp expr) {
			var f = new FunpDefine();
			f.isPolyType = isPolyType;
			f.var = var;
			f.value = value;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun4<Boolean, String, Funp, Funp, R> fun) {
			return fun.apply(isPolyType, var, value, expr);
		}
	}

	public static class FunpDefineGlobal implements Funp, P2.End {
		public String var;
		public Funp value;
		public Funp expr;

		public static FunpDefineGlobal of(String var, Funp value, Funp expr) {
			var f = new FunpDefineGlobal();
			f.var = var;
			f.value = value;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<String, Funp, Funp, R> fun) {
			return fun.apply(var, value, expr);
		}
	}

	public static class FunpDefineRec implements Funp, P2.End {
		public List<Pair<String, Funp>> pairs;
		public Funp expr;

		public static FunpDefineRec of(List<Pair<String, Funp>> pairs, Funp expr) {
			var f = new FunpDefineRec();
			f.pairs = pairs;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun2<List<Pair<String, Funp>>, Funp, R> fun) {
			return fun.apply(pairs, expr);
		}
	}

	public static class FunpDeref implements Funp, P2.End {
		public Funp pointer;

		public static FunpDeref of(Funp pointer) {
			var f = new FunpDeref();
			f.pointer = pointer;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(pointer);
		}
	}

	public static class FunpDontCare implements Funp, P4.End {
		public static FunpDontCare of() {
			return new FunpDontCare();
		}

		public <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
		}
	}

	public static class FunpError implements Funp, P4.End {
		public static FunpError of() {
			return new FunpError();
		}

		public <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
		}
	}

	public static class FunpField implements Funp, P2.End {
		public Funp reference;
		public String field;

		public static FunpField of(Funp reference, String field) {
			var f = new FunpField();
			f.reference = reference;
			f.field = field;
			return f;
		}

		public <R> R apply(FixieFun2<Funp, String, R> fun) {
			return fun.apply(reference, field);
		}
	}

	public static class FunpIf implements Funp, P4.End {
		public Funp if_;
		public Funp then;
		public Funp else_;

		public static FunpIf of(Funp if_, Funp then, Funp else_) {
			var f = new FunpIf();
			f.if_ = if_;
			f.then = then;
			f.else_ = else_;
			return f;
		}

		public <R> R apply(FixieFun3<Funp, Funp, Funp, R> fun) {
			return fun.apply(if_, then, else_);
		}
	}

	public static class FunpIndex implements Funp, P4.End {
		public FunpReference reference;
		public Funp index;

		public static FunpIndex of(FunpReference reference, Funp index) {
			var f = new FunpIndex();
			f.reference = reference;
			f.index = index;
			return f;
		}

		public <R> R apply(FixieFun2<FunpReference, Funp, R> fun) {
			return fun.apply(reference, index);
		}
	}

	public static class FunpIo implements Funp, P2.End {
		public Funp expr;

		public static FunpIo of(Funp expr) {
			var f = new FunpIo();
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(expr);
		}
	}

	public static class FunpIoAsm implements Funp, P4.End {
		public List<Pair<OpReg, Funp>> assigns;
		public List<Node> asm;

		public static FunpIoAsm of(List<Pair<OpReg, Funp>> assigns, List<Node> asm) {
			var f = new FunpIoAsm();
			f.assigns = assigns;
			f.asm = asm;
			return f;
		}

		public <R> R apply(FixieFun2<List<Pair<OpReg, Funp>>, List<Node>, R> fun) {
			return fun.apply(assigns, asm);
		}
	}

	public static class FunpIoAssignRef implements Funp, P2.End {
		public FunpReference reference;
		public Funp value;
		public Funp expr;

		public static FunpIoAssignRef of(FunpReference reference, Funp value, Funp expr) {
			var f = new FunpIoAssignRef();
			f.reference = reference;
			f.value = value;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<FunpReference, Funp, Funp, R> fun) {
			return fun.apply(reference, value, expr);
		}
	}

	public static class FunpIoCat implements Funp, P2.End {
		public Funp expr;

		public static FunpIoCat of(Funp expr) {
			var f = new FunpIoCat();
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(expr);
		}
	}

	public static class FunpIoFold implements Funp, P2.End {
		public Funp init; // expression
		public Funp cont; // lambda
		public Funp next; // lambda

		public static FunpIoFold of(Funp init, Funp cond, Funp next) {
			var f = new FunpIoFold();
			f.init = init;
			f.cont = cond;
			f.next = next;
			return f;
		}

		public <R> R apply(FixieFun3<Funp, Funp, Funp, R> fun) {
			return fun.apply(init, cont, next);
		}
	}

	public static class FunpIoWhile implements Funp, P4.End {
		public Funp while_;
		public Funp do_;
		public Funp expr;

		public static FunpIoWhile of(Funp while_, Funp do_, Funp expr) {
			var f = new FunpIoWhile();
			f.while_ = while_;
			f.do_ = do_;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<Funp, Funp, Funp, R> fun) {
			return fun.apply(while_, do_, expr);
		}
	}

	public static class FunpLambda implements Funp, P2.End {
		public String var;
		public Funp expr;

		public static FunpLambda of(String var, Funp expr) {
			var f = new FunpLambda();
			f.var = var;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun2<String, Funp, R> fun) {
			return fun.apply(var, expr);
		}
	}

	public static class FunpNumber implements Funp, P4.End {
		public IntMutable i;

		public static FunpNumber ofNumber(int i) {
			return of(IntMutable.of(i));
		}

		public static FunpNumber of(IntMutable i) {
			var f = new FunpNumber();
			f.i = i;
			return f;
		}

		public <R> R apply(FixieFun1<IntMutable, R> fun) {
			return fun.apply(i);
		}
	}

	public static class FunpPredefine implements Funp, P2.End {
		public Funp expr;

		public static FunpPredefine of(Funp expr) {
			var f = new FunpPredefine();
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(expr);
		}
	}

	public static class FunpReference implements Funp, P2.End {
		public Funp expr;

		public static FunpReference of(Funp expr) {
			var f = new FunpReference();
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(expr);
		}
	}

	public static class FunpRepeat implements Funp, P2.End {
		public int count;
		public Funp expr;

		public static FunpRepeat of(int count, Funp expr) {
			var f = new FunpRepeat();
			f.count = count;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun2<Integer, Funp, R> fun) {
			return fun.apply(count, expr);
		}
	}

	public static class FunpSizeOf implements Funp, P2.End {
		public Funp expr;

		public static FunpSizeOf of(Funp expr) {
			var f = new FunpSizeOf();
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(expr);
		}
	}

	public static class FunpStruct implements Funp, P2.End {
		public List<Pair<String, Funp>> pairs;

		public static FunpStruct of(List<Pair<String, Funp>> pairs) {
			var f = new FunpStruct();
			f.pairs = pairs;
			return f;
		}

		public <R> R apply(FixieFun1<List<Pair<String, Funp>>, R> fun) {
			return fun.apply(pairs);
		}
	}

	public static class FunpTree implements Funp, P4.End {
		public Operator operator;
		public Funp left;
		public Funp right;

		public static FunpTree of(Operator operator, Funp left, Funp right) {
			var f = new FunpTree();
			f.operator = operator;
			f.left = left;
			f.right = right;
			return f;
		}

		public <R> R apply(FixieFun3<Operator, Funp, Funp, R> fun) {
			return fun.apply(operator, left, right);
		}
	}

	public static class FunpTree2 implements Funp, P4.End {
		public Atom operator;
		public Funp left;
		public Funp right;

		public static FunpTree2 of(Atom operator, Funp left, Funp right) {
			var f = new FunpTree2();
			f.operator = operator;
			f.left = left;
			f.right = right;
			return f;
		}

		public static List<Funp> unfold(Funp n, Atom op) {
			var list = new ArrayList<Funp>();
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

	public static class FunpVariable implements Funp, P2.End {
		public String var;

		public static FunpVariable of(String var) {
			var f = new FunpVariable();
			f.var = var;
			return f;
		};

		public <R> R apply(FixieFun1<String, R> fun) {
			return fun.apply(var);
		}
	}

	public static class FunpVariableNew implements Funp, P2.End {
		public String var;

		public static FunpVariableNew of(String var) {
			var f = new FunpVariableNew();
			f.var = var;
			return f;
		};

		public <R> R apply(FixieFun1<String, R> fun) {
			return fun.apply(var);
		}
	}

}
