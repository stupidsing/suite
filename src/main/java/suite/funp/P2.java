package suite.funp;

import java.util.List;

import suite.adt.Mutable;
import suite.adt.pair.Fixie_.FixieFun0;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Fixie_.FixieFun4;
import suite.adt.pair.Pair;
import suite.assembler.Amd64.Operand;
import suite.funp.Funp_.Funp;
import suite.primitive.IntMutable;
import suite.primitive.adt.pair.IntIntPair;

public class P2 {

	public interface End {
	}

	public static class FunpAllocGlobal implements Funp, P4.End {
		public String var;
		public int size;
		public Funp expr;
		public Mutable<Operand> address;

		public static FunpAllocGlobal of(String var, int size, Funp expr, Mutable<Operand> address) {
			var f = new FunpAllocGlobal();
			f.var = var;
			f.size = size;
			f.expr = expr;
			f.address = address;
			return f;
		}

		public <R> R apply(FixieFun4<String, Integer, Funp, Mutable<Operand>, R> fun) {
			return fun.apply(var, size, expr, address);
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

	public static class FunpCmp implements Funp, P4.End {
		public boolean isEq;
		public FunpMemory left;
		public FunpMemory right;

		public static FunpCmp of(boolean isEq, FunpMemory left, FunpMemory right) {
			var f = new FunpCmp();
			f.isEq = isEq;
			f.left = left;
			f.right = right;
			return f;
		}

		public <R> R apply(FixieFun3<Boolean, FunpMemory, FunpMemory, R> fun) {
			return fun.apply(isEq, left, right);
		}
	}

	public static class FunpData implements Funp, P4.End {
		public List<Pair<Funp, IntIntPair>> pairs;

		public static FunpData of(List<Pair<Funp, IntIntPair>> pairs) {
			var f = new FunpData();
			f.pairs = pairs;
			return f;
		}

		public <R> R apply(FixieFun1<List<Pair<Funp, IntIntPair>>, R> fun) {
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

	public static class FunpInvoke implements Funp, P4.End {
		public Funp routine;

		public static FunpInvoke of(Funp routine) {
			var f = new FunpInvoke();
			f.routine = routine;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(routine);
		}
	}

	public static class FunpInvoke2 implements Funp, P4.End {
		public Funp routine;

		public static FunpInvoke2 of(Funp routine) {
			var f = new FunpInvoke2();
			f.routine = routine;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(routine);
		}
	}

	public static class FunpInvokeIo implements Funp, P4.End {
		public Funp routine;
		public int is, os;

		public static FunpInvokeIo of(Funp routine, int is, int os) {
			var f = new FunpInvokeIo();
			f.routine = routine;
			f.is = is;
			f.os = os;
			return f;
		}

		public <R> R apply(FixieFun3<Funp, Integer, Integer, R> fun) {
			return fun.apply(routine, is, os);
		}
	}

	public static class FunpLambdaCapture implements Funp, P2.End {
		public String var;
		public String capn;
		public Funp cap;
		public Funp expr;

		public static FunpLambdaCapture of(String var, String capn, Funp cap, Funp expr) {
			var f = new FunpLambdaCapture();
			f.var = var;
			f.capn = capn;
			f.cap = cap;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun4<String, String, Funp, Funp, R> fun) {
			return fun.apply(var, capn, cap, expr);
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

		public int size() {
			return end - start;
		}

		public <R> R apply(FixieFun3<Funp, Integer, Integer, R> fun) {
			return fun.apply(pointer, start, end);
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

	public static class FunpRoutine implements Funp, P4.End {
		public Funp frame;
		public Funp expr;

		public static FunpRoutine of(Funp frame, Funp expr) {
			var f = new FunpRoutine();
			f.frame = frame;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun2<Funp, Funp, R> fun) {
			return fun.apply(frame, expr);
		}
	}

	public static class FunpRoutine2 implements Funp, P4.End {
		public Funp frame;
		public Funp expr;

		public static FunpRoutine2 of(Funp frame, Funp expr) {
			var f = new FunpRoutine2();
			f.frame = frame;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun2<Funp, Funp, R> fun) {
			return fun.apply(frame, expr);
		}

	}

	public static class FunpRoutineIo implements Funp, P4.End {
		public Funp frame;
		public Funp expr;
		public int is, os;

		public static FunpRoutineIo of(Funp frame, Funp expr, int is, int os) {
			var f = new FunpRoutineIo();
			f.frame = frame;
			f.expr = expr;
			f.is = is;
			f.os = os;
			return f;
		}

		public <R> R apply(FixieFun4<Funp, Funp, Integer, Integer, R> fun) {
			return fun.apply(frame, expr, is, os);
		}
	}

	public static class FunpSaveRegisters implements Funp, P4.End {
		public Funp expr;

		public static FunpSaveRegisters of(Funp expr) {
			var f = new FunpSaveRegisters();
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(expr);
		}
	}

}
