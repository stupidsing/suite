package suite.funp;

import suite.funp.Funp_.Funp;

public class P1 {

	public interface End {
	}

	public static class FunpAllocStack implements Funp, P2.End {
		public int size; // allocate size
		public Funp value;
		public Funp expr;

		public static FunpAllocStack of(int size, Funp value, Funp expr) {
			FunpAllocStack f = new FunpAllocStack();
			f.size = size;
			f.value = value;
			f.expr = expr;
			return f;
		}
	}

	public static class FunpAssign implements Funp, P2.End {
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
	}

	public static class FunpFramePointer implements Funp, P2.End {
	}

	public static class FunpInvokeInt implements Funp, P2.End {
		public Funp routine;

		public static FunpInvokeInt of(Funp routine) {
			FunpInvokeInt f = new FunpInvokeInt();
			f.routine = routine;
			return f;
		}
	}

	public static class FunpInvokeInt2 implements Funp, P2.End {
		public Funp routine;

		public static FunpInvokeInt2 of(Funp routine) {
			FunpInvokeInt2 f = new FunpInvokeInt2();
			f.routine = routine;
			return f;
		}
	}

	public static class FunpInvokeIo implements Funp, P2.End {
		public Funp routine;

		public static FunpInvokeIo of(Funp routine) {
			FunpInvokeIo f = new FunpInvokeIo();
			f.routine = routine;
			return f;
		}
	}

	public static class FunpMemory implements Funp, P2.End {
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
	}

	public static class FunpRoutine implements Funp, P2.End {
		public Funp expr;

		public static FunpRoutine of(Funp expr) {
			FunpRoutine f = new FunpRoutine();
			f.expr = expr;
			return f;
		}
	}

	public static class FunpRoutine2 implements Funp, P2.End {
		public Funp expr;

		public static FunpRoutine2 of(Funp expr) {
			FunpRoutine2 f = new FunpRoutine2();
			f.expr = expr;
			return f;
		}
	}

	public static class FunpRoutineIo implements Funp, P2.End {
		public Funp expr;
		public int is, os;

		public static FunpRoutineIo of(Funp expr, int is, int os) {
			FunpRoutineIo f = new FunpRoutineIo();
			f.expr = expr;
			f.is = is;
			f.os = os;
			return f;
		}
	}

	public static class FunpSaveRegisters implements Funp, P2.End {
		public Funp expr;

		public static FunpSaveRegisters of(Funp expr) {
			FunpSaveRegisters f = new FunpSaveRegisters();
			f.expr = expr;
			return f;
		}
	}

}
