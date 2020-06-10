package suite.funp;

import java.util.ArrayList;
import java.util.List;

import primal.adt.Fixie_.FixieFun0;
import primal.adt.Fixie_.FixieFun1;
import primal.adt.Fixie_.FixieFun2;
import primal.adt.Fixie_.FixieFun3;
import primal.adt.Fixie_.FixieFun4;
import primal.adt.Pair;
import primal.parser.Operator;
import primal.primitive.adt.IntMutable;
import suite.assembler.Amd64.OpReg;
import suite.funp.Funp_.Funp;
import suite.node.Atom;
import suite.node.Node;

public class P0 {

	public interface End {
	}

	public enum Coerce {
		BYTE(1), // 8 bits
		NUMBERP(Funp_.pointerSize), // a number with same size as a machine level pointer
		NUMBER(Funp_.integerSize), // a number with same size as a machine level generic register, 32-bits
		POINTER(Funp_.pointerSize), // a machine level pointer
		;

		public final int size;

		Coerce(int size) {
			this.size = size;
		}
	};

	public static class FunpAdjustArrayPointer implements Funp, P2.End {
		public Funp pointer;
		public Funp adjust;

		public static FunpAdjustArrayPointer of(Funp pointer, Funp adjust) {
			var f = new FunpAdjustArrayPointer();
			f.pointer = pointer;
			f.adjust = adjust;
			return f;
		}

		public <R> R apply(FixieFun2<Funp, Funp, R> fun) {
			return fun.apply(pointer, adjust);
		}
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

	public static class FunpCoerce implements Funp, P2.End {
		public Coerce from;
		public Coerce to;
		public Funp expr;

		public static FunpCoerce of(Coerce from, Coerce to, Funp expr) {
			var f = new FunpCoerce();
			f.from = from;
			f.to = to;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<Coerce, Coerce, Funp, R> fun) {
			return fun.apply(from, to, expr);
		}
	}

	public static class FunpDefine implements Funp, P2.End {
		public String vn;
		public Funp value;
		public Funp expr;
		public Fdt fdt;

		public static FunpDefine of(String vn, Funp value, Funp expr, Fdt fdt) {
			var f = new FunpDefine();
			f.vn = vn;
			f.value = value;
			f.expr = expr;
			f.fdt = fdt;
			return f;
		}

		public <R> R apply(FixieFun4<String, Funp, Funp, Fdt, R> fun) {
			return fun.apply(vn, value, expr, fdt);
		}
	}

	public static class FunpDefineRec implements Funp, P2.End {
		public List<Pair<String, Funp>> pairs;
		public Funp expr;
		public Fdt fdt;

		public static FunpDefineRec of(List<Pair<String, Funp>> pairs, Funp expr, Fdt fdt) {
			var f = new FunpDefineRec();
			f.pairs = pairs;
			f.expr = expr;
			f.fdt = fdt;
			return f;
		}

		public <R> R apply(FixieFun3<List<Pair<String, Funp>>, Funp, Fdt, R> fun) {
			return fun.apply(pairs, expr, fdt);
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

	public static class FunpDoAsm implements Funp, P4.End {
		public List<Pair<OpReg, Funp>> assigns;
		public List<Node> asm;
		public OpReg opResult;

		public static FunpDoAsm of(List<Pair<OpReg, Funp>> assigns, List<Node> asm, OpReg opResult) {
			var f = new FunpDoAsm();
			f.assigns = assigns;
			f.asm = asm;
			f.opResult = opResult;
			return f;
		}

		public <R> R apply(FixieFun3<List<Pair<OpReg, Funp>>, List<Node>, OpReg, R> fun) {
			return fun.apply(assigns, asm, opResult);
		}
	}

	public static class FunpDoAssignRef implements Funp, P2.End {
		public FunpReference reference;
		public Funp value;
		public Funp expr;

		public static FunpDoAssignRef of(FunpReference reference, Funp value, Funp expr) {
			var f = new FunpDoAssignRef();
			f.reference = reference;
			f.value = value;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<FunpReference, Funp, Funp, R> fun) {
			return fun.apply(reference, value, expr);
		}
	}

	public static class FunpDoAssignVar implements Funp, P2.End {
		public FunpVariable var;
		public Funp value;
		public Funp expr;

		public static FunpDoAssignVar of(FunpVariable var, Funp value, Funp expr) {
			var f = new FunpDoAssignVar();
			f.var = var;
			f.value = value;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<FunpVariable, Funp, Funp, R> fun) {
			return fun.apply(var, value, expr);
		}
	}

	public static class FunpDoEvalIo implements Funp, P2.End {
		public Funp expr;

		public static FunpDoEvalIo of(Funp expr) {
			var f = new FunpDoEvalIo();
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(expr);
		}
	}

	public static class FunpDoFold implements Funp, P2.End {
		public Funp init; // expression
		public Funp cont; // lambda
		public Funp next; // lambda

		public static FunpDoFold of(Funp init, Funp cond, Funp next) {
			var f = new FunpDoFold();
			f.init = init;
			f.cont = cond;
			f.next = next;
			return f;
		}

		public <R> R apply(FixieFun3<Funp, Funp, Funp, R> fun) {
			return fun.apply(init, cont, next);
		}
	}

	public static class FunpDoHeapDel implements Funp, P4.End {
		public boolean isDynamicSize;
		public Funp reference;
		public Funp expr;

		public static FunpDoHeapDel of(boolean isDynamicSize, Funp reference, Funp expr) {
			var f = new FunpDoHeapDel();
			f.isDynamicSize = isDynamicSize;
			f.reference = reference;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<Boolean, Funp, Funp, R> fun) {
			return fun.apply(isDynamicSize, reference, expr);
		}
	}

	public static class FunpDoHeapNew implements Funp, P4.End {

		// if true, may deallocate with any sizes by using -1 in FunpHeapDealloc.size
		public boolean isDynamicSize;

		public static FunpDoHeapNew of(boolean isDynamicSize) {
			var f = new FunpDoHeapNew();
			f.isDynamicSize = isDynamicSize;
			return f;
		}

		public <R> R apply(FixieFun1<Boolean, R> fun) {
			return fun.apply(isDynamicSize);
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

	public static class FunpDoWhile implements Funp, P4.End {
		public Funp while_;
		public Funp do_;
		public Funp expr;

		public static FunpDoWhile of(Funp while_, Funp do_, Funp expr) {
			var f = new FunpDoWhile();
			f.while_ = while_;
			f.do_ = do_;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<Funp, Funp, Funp, R> fun) {
			return fun.apply(while_, do_, expr);
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
		public FunpReference reference;
		public String field;

		public static FunpField of(FunpReference reference, String field) {
			var f = new FunpField();
			f.reference = reference;
			f.field = field;
			return f;
		}

		public <R> R apply(FixieFun2<FunpReference, String, R> fun) {
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

	public static class FunpLambda implements Funp, P2.End {
		public String vn;
		public Funp expr;
		public Fct fct;
		public String name; // optional

		public static FunpLambda of(String vn, Funp expr) {
			return of(vn, expr, null);
		}

		public static FunpLambda of(String vn, Funp expr, Fct fct) {
			var f = new FunpLambda();
			f.vn = vn;
			f.expr = expr;
			f.fct = fct;
			return f;
		}

		public <R> R apply(FixieFun3<String, Funp, Fct, R> fun) {
			return fun.apply(vn, expr, fct);
		}
	}

	public static class FunpLambdaFree implements Funp, P2.End {
		public Funp lambda;
		public Funp expr;

		public static FunpLambdaFree of(Funp lambda, Funp expr) {
			var f = new FunpLambdaFree();
			f.lambda = lambda;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun2<Funp, Funp, R> fun) {
			return fun.apply(lambda, expr);
		}
	}

	public static class FunpMe implements Funp, P4.End {
		public static FunpMe of() {
			return new FunpMe();
		}

		public <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
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
		public String vn;
		public Funp expr;

		public static FunpPredefine of(String vn, Funp expr) {
			var f = new FunpPredefine();
			f.vn = vn;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun2<String, Funp, R> fun) {
			return fun.apply(vn, expr);
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

	public static class FunpRemark implements Funp, P4.End {
		public String remark;
		public Funp expr;

		public static FunpRemark of(String remark, Funp expr) {
			var f = new FunpRemark();
			f.remark = remark;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun2<String, Funp, R> fun) {
			return fun.apply(remark, expr);
		}
	}

	public static class FunpRepeat implements Funp, P2.End {
		public Integer count;
		public Funp expr;

		public static FunpRepeat of(Integer count, Funp expr) {
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

	public static class FunpTag implements Funp, P2.End {
		public IntMutable id;
		public String tag;
		public Funp value;

		public static FunpTag of(IntMutable id, String tag, Funp value) {
			var f = new FunpTag();
			f.id = id;
			f.tag = tag;
			f.value = value;
			return f;
		}

		public <R> R apply(FixieFun3<IntMutable, String, Funp, R> fun) {
			return fun.apply(id, tag, value);
		}
	}

	public static class FunpTagId implements Funp, P2.End {
		public FunpReference reference;

		public static FunpTagId of(FunpReference reference) {
			var f = new FunpTagId();
			f.reference = reference;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(reference);
		}
	}

	public static class FunpTagValue implements Funp, P2.End {
		public FunpReference reference;
		public String tag;

		public static FunpTagValue of(FunpReference reference, String tag) {
			var f = new FunpTagValue();
			f.reference = reference;
			f.tag = tag;
			return f;
		}

		public <R> R apply(FixieFun2<Funp, String, R> fun) {
			return fun.apply(reference, tag);
		}
	}

	public static class FunpTree implements Funp, P2.End {
		public Operator operator;
		public Funp left;
		public Funp right;
		public Coerce size;

		public static FunpTree of(Operator operator, Funp left, Funp right) {
			return of(operator, left, right, Coerce.NUMBER);
		}

		public static FunpTree of(Operator operator, Funp left, Funp right, Coerce size) {
			var f = new FunpTree();
			f.operator = operator;
			f.left = left;
			f.right = right;
			f.size = size;
			return f;
		}

		public <R> R apply(FixieFun4<Operator, Funp, Funp, Coerce, R> fun) {
			return fun.apply(operator, left, right, size);
		}
	}

	public static class FunpTree2 implements Funp, P2.End {
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

	public static class FunpTypeCheck implements Funp, P4.End {
		public Funp left;
		public Funp right;
		public Funp expr;

		public static FunpTypeCheck of(Funp left, Funp right, Funp expr) {
			var f = new FunpTypeCheck();
			f.left = left;
			f.right = right;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<Funp, Funp, Funp, R> fun) {
			return fun.apply(left, right, expr);
		}
	}

	public static class FunpVariable implements Funp, P2.End {
		public String vn;

		public static FunpVariable of(String vn) {
			var f = new FunpVariable();
			f.vn = vn;
			return f;
		};

		public <R> R apply(FixieFun1<String, R> fun) {
			return fun.apply(vn);
		}
	}

	public static class FunpVariableNew implements Funp, P2.End {
		public String vn;

		public static FunpVariableNew of(String vn) {
			var f = new FunpVariableNew();
			f.vn = vn;
			return f;
		};

		public <R> R apply(FixieFun1<String, R> fun) {
			return fun.apply(vn);
		}
	}

	public enum Fct { // capture type
		MANUAL, // to be uncaptured (freed) manually
		NOSCOP, // do not access variables of outer scope
		ONCE__, // to be freed after first call
	}

	public enum Fdt {
		G_MONO, // global variable, mono type
		G_POLY, // global variable, polymorphic type
		L_IOAP, // local variable, I/O access
		L_MONO, // local variable, mono type
		L_POLY, // local variable, polymorphic type
		S_MONO, // substitution variable, mono type
		S_POLY, // substitution variable, polymorphic type
		VIRT, // virtual variable
		;

		public static boolean isGlobal(Fdt fdt) {
			return fdt == Fdt.G_MONO || fdt == Fdt.G_POLY;
		}

		public static boolean isLocal(Fdt fdt) {
			return fdt == Fdt.L_IOAP || fdt == Fdt.L_MONO || fdt == Fdt.L_POLY;
		}

		public static boolean isPoly(Fdt fdt) {
			return fdt == Fdt.G_POLY || fdt == Fdt.L_POLY || fdt == Fdt.S_POLY;
		}

		public static boolean isSubs(Fdt fdt) {
			return fdt == Fdt.S_MONO || fdt == Fdt.S_POLY;
		}
	};

}
