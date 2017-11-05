package suite.funp;

import java.util.List;

import suite.adt.Mutable;
import suite.adt.pair.Fixie_.FixieFun0;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Fixie_.FixieFun4;
import suite.adt.pair.Pair;
import suite.funp.Funp_.Funp;
import suite.primitive.adt.pair.IntIntPair;

public class P1 {

	public interface End {
	}

	public static class FunpAllocStack implements Funp, P3.End {
		public int size; // allocate size
		public Funp value;
		public Funp expr;
		public Mutable<Integer> stack;

		public static FunpAllocStack of(int size, Funp value, Funp expr, Mutable<Integer> stack) {
			FunpAllocStack f = new FunpAllocStack();
			f.size = size;
			f.value = value;
			f.expr = expr;
			f.stack = stack;
			return f;
		}

		public <R> R apply(FixieFun4<Integer, Funp, Funp, Mutable<Integer>, R> fun) {
			return fun.apply(size, value, expr, stack);
		}
	}

	public static class FunpAssign implements Funp, P3.End {
		public FunpMemory memory;
		public Funp value;
		public Funp expr;

		public static FunpAssign of(FunpMemory memory, Funp value, Funp expr) {
			FunpAssign f = new FunpAssign();
			f.memory = memory;
			f.value = value;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<FunpMemory, Funp, Funp, R> fun) {
			return fun.apply(memory, value, expr);
		}
	}

	public static class FunpData implements Funp, P3.End {
		public List<Pair<Funp, IntIntPair>> pairs;

		public static FunpData of(List<Pair<Funp, IntIntPair>> pairs) {
			FunpData f = new FunpData();
			f.pairs = pairs;
			return f;
		}

		public <R> R apply(FixieFun1<List<Pair<Funp, IntIntPair>>, R> fun) {
			return fun.apply(pairs);
		}
	}

	public static class FunpFramePointer implements Funp, P3.End {
		public <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
		}
	}

	public static class FunpInvokeInt implements Funp, P3.End {
		public Funp routine;

		public static FunpInvokeInt of(Funp routine) {
			FunpInvokeInt f = new FunpInvokeInt();
			f.routine = routine;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(routine);
		}
	}

	public static class FunpInvokeInt2 implements Funp, P3.End {
		public Funp routine;

		public static FunpInvokeInt2 of(Funp routine) {
			FunpInvokeInt2 f = new FunpInvokeInt2();
			f.routine = routine;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(routine);
		}
	}

	public static class FunpInvokeIo implements Funp, P3.End {
		public Funp routine;

		public static FunpInvokeIo of(Funp routine) {
			FunpInvokeIo f = new FunpInvokeIo();
			f.routine = routine;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(routine);
		}
	}

	public static class FunpMemory implements Funp, P3.End {
		public Funp pointer;
		public int start;
		public int end;

		public static FunpMemory of(Funp pointer, int start, int end) {
			FunpMemory f = new FunpMemory();
			f.pointer = pointer;
			f.start = start;
			f.end = end;
			return f;
		}

		public FunpMemory range(int s, int e) {
			return of(pointer, start + s, start + e);
		}

		public int size() {
			return end - start;
		}

		public <R> R apply(FixieFun3<Funp, Integer, Integer, R> fun) {
			return fun.apply(pointer, start, end);
		}
	}

	public static class FunpRoutine implements Funp, P3.End {
		public Funp expr;

		public static FunpRoutine of(Funp expr) {
			FunpRoutine f = new FunpRoutine();
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(expr);
		}
	}

	public static class FunpRoutine2 implements Funp, P3.End {
		public Funp expr;

		public static FunpRoutine2 of(Funp expr) {
			FunpRoutine2 f = new FunpRoutine2();
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(expr);
		}

	}

	public static class FunpRoutineIo implements Funp, P3.End {
		public Funp expr;
		public int is, os;

		public static FunpRoutineIo of(Funp expr, int is, int os) {
			FunpRoutineIo f = new FunpRoutineIo();
			f.expr = expr;
			f.is = is;
			f.os = os;
			return f;
		}

		public <R> R apply(FixieFun3<Funp, Integer, Integer, R> fun) {
			return fun.apply(expr, is, os);
		}
	}

	public static class FunpSaveRegisters implements Funp, P3.End {
		public Funp expr;

		public static FunpSaveRegisters of(Funp expr) {
			FunpSaveRegisters f = new FunpSaveRegisters();
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(expr);
		}
	}

	public static class FunpWhile implements Funp, P3.End {
		public Funp while_;
		public Funp do_;
		public Funp expr;

		public static FunpWhile of(Funp while_, Funp do_, Funp expr) {
			FunpWhile f = new FunpWhile();
			f.while_ = while_;
			f.do_ = do_;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<Funp, Funp, Funp, R> fun) {
			return fun.apply(while_, do_, expr);
		}
	}

}
