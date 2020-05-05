package suite.funp;

import java.util.ArrayList;
import java.util.List;

import primal.adt.Fixie_.FixieFun0;
import primal.adt.Fixie_.FixieFun1;
import primal.adt.Fixie_.FixieFun2;
import primal.adt.Fixie_.FixieFun3;
import primal.adt.Fixie_.FixieFun4;
import primal.adt.Fixie_.FixieFun5;
import primal.adt.Fixie_.FixieFun6;
import primal.adt.Mutable;
import primal.adt.Pair;
import primal.parser.Operator;
import primal.primitive.adt.IntMutable;
import primal.primitive.adt.IntRange;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.funp.Funp_.Funp;
import suite.funp.P0.Fct;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpVariable;

public class P2 {

	public interface End {
	}

	public static class FunpAllocGlobal implements Funp, P4.End {
		public int size;
		public Funp value;
		public Funp expr;
		public Mutable<Operand> address;

		public static FunpAllocGlobal of(int size, Funp value, Funp expr, Mutable<Operand> address) {
			var f = new FunpAllocGlobal();
			f.size = size;
			f.value = value;
			f.expr = expr;
			f.address = address;
			return f;
		}

		public <R> R apply(FixieFun4<Integer, Funp, Funp, Mutable<Operand>, R> fun) {
			return fun.apply(size, value, expr, address);
		}
	}

	public static class FunpAllocReg implements Funp, P4.End {
		public int size;
		public Funp value;
		public Funp expr;
		public Mutable<Operand> reg;

		public static FunpAllocReg of(int size, Funp value, Funp expr, Mutable<Operand> reg) {
			var f = new FunpAllocReg();
			f.size = size;
			f.value = value;
			f.expr = expr;
			f.reg = reg;
			return f;
		}

		public <R> R apply(FixieFun4<Integer, Funp, Funp, Mutable<Operand>, R> fun) {
			return fun.apply(size, value, expr, reg);
		}
	}

	public static class FunpAllocStack implements Funp, P4.End {
		public int size;
		public Funp value;
		public Funp expr;
		public IntMutable stack;

		public static FunpAllocStack of(int size, Funp value, Funp expr, IntMutable stack) {
			var f = new FunpAllocStack();
			f.size = size;
			f.value = value;
			f.expr = expr;
			f.stack = stack;
			return f;
		}

		public <R> R apply(FixieFun4<Integer, Funp, Funp, IntMutable, R> fun) {
			return fun.apply(size, value, expr, stack);
		}
	}

	public static class FunpAssignMem implements Funp, P4.End {
		public FunpMemory target;
		public Funp value;
		public Funp expr;

		public static FunpAssignMem of(FunpMemory target, Funp value, Funp expr) {
			var f = new FunpAssignMem();
			f.target = target;
			f.value = value;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<FunpMemory, Funp, Funp, R> fun) {
			return fun.apply(target, value, expr);
		}
	}

	public static class FunpAssignOp implements Funp, P4.End {
		public FunpOperand target;
		public Funp value;
		public Funp expr;

		public static FunpAssignOp of(FunpOperand target, Funp value, Funp expr) {
			var f = new FunpAssignOp();
			f.target = target;
			f.value = value;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<FunpOperand, Funp, Funp, R> fun) {
			return fun.apply(target, value, expr);
		}
	}

	public static class FunpAssignOp2 implements Funp, P4.End {
		public FunpOperand2 target;
		public Funp value;
		public Funp expr;

		public static FunpAssignOp2 of(FunpOperand2 target, Funp value, Funp expr) {
			var f = new FunpAssignOp2();
			f.target = target;
			f.value = value;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<FunpOperand2, Funp, Funp, R> fun) {
			return fun.apply(target, value, expr);
		}
	}

	public static class FunpCmp implements Funp, P4.End {
		public Operator operator;
		public FunpMemory left;
		public FunpMemory right;

		public static FunpCmp of(Operator operator, FunpMemory left, FunpMemory right) {
			var f = new FunpCmp();
			f.operator = operator;
			f.left = left;
			f.right = right;
			return f;
		}

		public <R> R apply(FixieFun3<Operator, FunpMemory, FunpMemory, R> fun) {
			return fun.apply(operator, left, right);
		}
	}

	public static class FunpData implements Funp, P4.End {
		public List<Pair<Funp, IntRange>> pairs;

		public static FunpData of(List<Pair<Funp, IntRange>> pairs) {
			var f = new FunpData();
			f.pairs = pairs;
			return f;
		}

		public <R> R apply(FixieFun1<List<Pair<Funp, IntRange>>, R> fun) {
			return fun.apply(pairs);
		}
	}

	public static class FunpFramePointer implements Funp, P4.End {
		public static FunpFramePointer of() {
			return new FunpFramePointer();
		}

		public <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
		}
	}

	public static class FunpHeapAlloc implements Funp, P4.End {
		public boolean isDynamicSize;
		public int size;

		public static FunpHeapAlloc of(boolean isDynamicSize, int size) {
			var f = new FunpHeapAlloc();
			f.isDynamicSize = isDynamicSize;
			f.size = size;
			return f;
		}

		public <R> R apply(FixieFun2<Boolean, Integer, R> fun) {
			return fun.apply(isDynamicSize, size);
		}
	}

	public static class FunpHeapDealloc implements Funp, P4.End {
		public boolean isDynamicSize;
		public int size;
		public Funp reference;
		public Funp expr;

		public static FunpHeapDealloc of(boolean isDynamicSize, int size, Funp reference, Funp expr) {
			var f = new FunpHeapDealloc();
			f.isDynamicSize = isDynamicSize;
			f.size = size;
			f.reference = reference;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun4<Boolean, Integer, Funp, Funp, R> fun) {
			return fun.apply(isDynamicSize, size, reference, expr);
		}
	}

	public static class FunpInvoke1 implements Funp, P4.End {
		public Funp routine;
		public int is;
		public int os;
		public int istack;
		public int ostack;

		public static FunpInvoke1 of(Funp routine, int is, int os, int istack, int ostack) {
			var f = new FunpInvoke1();
			f.routine = routine;
			f.is = is;
			f.os = os;
			f.istack = istack;
			f.ostack = ostack;
			return f;
		}

		public <R> R apply(FixieFun5<Funp, Integer, Integer, Integer, Integer, R> fun) {
			return fun.apply(routine, is, os, istack, ostack);
		}
	}

	public static class FunpInvoke2 implements Funp, P4.End {
		public Funp routine;
		public int is;
		public int os;
		public int istack;
		public int ostack;

		public static FunpInvoke2 of(Funp routine, int is, int os, int istack, int ostack) {
			var f = new FunpInvoke2();
			f.routine = routine;
			f.is = is;
			f.os = os;
			f.istack = istack;
			f.ostack = ostack;
			return f;
		}

		public <R> R apply(FixieFun5<Funp, Integer, Integer, Integer, Integer, R> fun) {
			return fun.apply(routine, is, os, istack, ostack);
		}
	}

	public static class FunpInvokeIo implements Funp, P4.End {
		public Funp routine;
		public int is;
		public int os;
		public int istack;
		public int ostack;

		public static FunpInvokeIo of(Funp routine, int is, int os, int istack, int ostack) {
			var f = new FunpInvokeIo();
			f.routine = routine;
			f.is = is;
			f.os = os;
			f.istack = istack;
			f.ostack = ostack;
			return f;
		}

		public <R> R apply(FixieFun5<Funp, Integer, Integer, Integer, Integer, R> fun) {
			return fun.apply(routine, is, os, istack, ostack);
		}
	}

	public static class FunpLambdaCapture implements Funp, P2.End {
		public FunpVariable fpIn;
		public FunpVariable frameVar;
		public FunpStruct struct;
		public String vn;
		public Funp expr;
		public Fct fct;

		public static FunpLambdaCapture of( //
				FunpVariable fpIn, //
				FunpVariable frameVar, //
				FunpStruct struct, //
				String vn, //
				Funp expr, //
				Fct fct) {
			var f = new FunpLambdaCapture();
			f.fpIn = fpIn;
			f.frameVar = frameVar;
			f.struct = struct;
			f.vn = vn;
			f.expr = expr;
			f.fct = fct;
			return f;
		}

		public <R> R apply(FixieFun6<FunpVariable, FunpVariable, FunpStruct, String, Funp, Fct, R> fun) {
			return fun.apply(fpIn, frameVar, struct, vn, expr, fct);
		}
	}

	public static class FunpMemory implements Funp, P4.End {
		public Funp pointer;
		public int start;
		public int end;

		public static FunpMemory of(Funp pointer, int start, int end) {
			var f = new FunpMemory();
			f.pointer = pointer;
			f.start = start;
			f.end = end;
			return f;
		}

		public <R> R apply(FixieFun3<Funp, Integer, Integer, R> fun) {
			return fun.apply(pointer, start, end);
		}

		public int size() {
			return end - start;
		}
	}

	public static class FunpOp implements Funp, P4.End {
		public int opSize;
		public Object operator;
		public Funp left;
		public Funp right;

		public static FunpOp of(int opSize, Object operator, Funp left, Funp right) {
			var f = new FunpOp();
			f.opSize = opSize;
			f.operator = operator;
			f.left = left;
			f.right = right;
			return f;
		}

		public <R> R apply(FixieFun4<Integer, Object, Funp, Funp, R> fun) {
			return fun.apply(opSize, operator, left, right);
		}
	}

	public static class FunpOperand implements Funp, P4.End {
		public Mutable<Operand> operand;

		public static FunpOperand of(Mutable<Operand> operand) {
			var f = new FunpOperand();
			f.operand = operand;
			return f;
		}

		public <R> R apply(FixieFun1<Mutable<Operand>, R> fun) {
			return fun.apply(operand);
		}
	}

	public static class FunpOperand2 implements Funp, P4.End {
		public Mutable<Operand> operand0;
		public Mutable<Operand> operand1;

		public static FunpOperand2 of(Mutable<Operand> operand0, Mutable<Operand> operand1) {
			var f = new FunpOperand2();
			f.operand0 = operand0;
			f.operand1 = operand1;
			return f;
		}

		public <R> R apply(FixieFun2<Mutable<Operand>, Mutable<Operand>, R> fun) {
			return fun.apply(operand0, operand1);
		}
	}

	public static class FunpRoutine1 implements Funp, P4.End {
		public Funp frame;
		public Funp expr;
		public int is;
		public int os;
		public int istack;
		public int ostack;

		public static FunpRoutine1 of(Funp frame, Funp expr, int is, int os, int istack, int ostack) {
			var f = new FunpRoutine1();
			f.frame = frame;
			f.expr = expr;
			f.is = is;
			f.os = os;
			f.istack = istack;
			f.ostack = ostack;
			return f;
		}

		public <R> R apply(FixieFun6<Funp, Funp, Integer, Integer, Integer, Integer, R> fun) {
			return fun.apply(frame, expr, is, os, istack, ostack);
		}
	}

	public static class FunpRoutine2 implements Funp, P4.End {
		public Funp frame;
		public Funp expr;
		public int is;
		public int os;
		public int istack;
		public int ostack;

		public static FunpRoutine2 of(Funp frame, Funp expr, int is, int os, int istack, int ostack) {
			var f = new FunpRoutine2();
			f.frame = frame;
			f.expr = expr;
			f.is = is;
			f.os = os;
			f.istack = istack;
			f.ostack = ostack;
			return f;
		}

		public <R> R apply(FixieFun6<Funp, Funp, Integer, Integer, Integer, Integer, R> fun) {
			return fun.apply(frame, expr, is, os, istack, ostack);
		}
	}

	public static class FunpRoutineIo implements Funp, P4.End {
		public Funp frame;
		public Funp expr;
		public int is;
		public int os;
		public int istack;
		public int ostack;

		public static FunpRoutineIo of(Funp frame, Funp expr, int is, int os, int istack, int ostack) {
			var f = new FunpRoutineIo();
			f.frame = frame;
			f.expr = expr;
			f.is = is;
			f.os = os;
			f.istack = istack;
			f.ostack = ostack;
			return f;
		}

		public <R> R apply(FixieFun6<Funp, Funp, Integer, Integer, Integer, Integer, R> fun) {
			return fun.apply(frame, expr, is, os, istack, ostack);
		}
	}

	public static class FunpSaveRegisters0 implements Funp, P4.End {
		public Funp expr;
		public Mutable<ArrayList<Pair<OpReg, Integer>>> saves;

		public static FunpSaveRegisters0 of(Funp expr, Mutable<ArrayList<Pair<OpReg, Integer>>> saves) {
			var f = new FunpSaveRegisters0();
			f.expr = expr;
			f.saves = saves;
			return f;
		}

		public <R> R apply(FixieFun2<Funp, Mutable<ArrayList<Pair<OpReg, Integer>>>, R> fun) {
			return fun.apply(expr, saves);
		}
	}

	public static class FunpSaveRegisters1 implements Funp, P4.End {
		public Funp expr;
		public Mutable<ArrayList<Pair<OpReg, Integer>>> saves;

		public static FunpSaveRegisters1 of(Funp expr, Mutable<ArrayList<Pair<OpReg, Integer>>> saves) {
			var f = new FunpSaveRegisters1();
			f.expr = expr;
			f.saves = saves;
			return f;
		}

		public <R> R apply(FixieFun2<Funp, Mutable<ArrayList<Pair<OpReg, Integer>>>, R> fun) {
			return fun.apply(expr, saves);
		}
	}

	public static class FunpTypeAssign implements Funp, P2.End {
		public FunpVariable left;
		public Funp right;
		public Funp expr;

		public static FunpTypeAssign of(FunpVariable left, Funp right, Funp expr) {
			var f = new FunpTypeAssign();
			f.left = left;
			f.right = right;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<FunpVariable, Funp, Funp, R> fun) {
			return fun.apply(left, right, expr);
		}
	}

}
