package suite.funp;

import suite.funp.Funp_.Funp;
import suite.util.FunUtil.Fun;

public class P1 {

	public interface End {
	}

	public static class FunpAllocStack implements Funp, P2.End {
		public int size; // allocate size
		public Fun<FunpMemory, Funp> expr;

		public static FunpAllocStack of(int size, Fun<FunpMemory, Funp> expr) {
			FunpAllocStack f = new FunpAllocStack();
			f.size = size;
			f.expr = expr;
			return f;
		}
	}

	public static class FunpAssign implements Funp, P2.End {
		public FunpMemory memory;
		public Funp value;

		public static FunpAssign of(FunpMemory memory, Funp value) {
			FunpAssign f = new FunpAssign();
			f.memory = memory;
			f.value = value;
			return f;
		}
	}

	public static class FunpFramePointer implements Funp, P2.End {
	}

	public static class FunpInvoke implements Funp, P2.End {
		public Funp lambda;

		public static FunpInvoke of(Funp lambda) {
			FunpInvoke f = new FunpInvoke();
			f.lambda = lambda;
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

	public static class FunpSaveFramePointer implements Funp, P2.End {
		public Funp expr;

		public static FunpSaveFramePointer of(Funp expr) {
			FunpSaveFramePointer f = new FunpSaveFramePointer();
			f.expr = expr;
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

	public static class FunpSeq implements Funp, P2.End {
		public Funp[] exprs;

		public static FunpSeq of(Funp... exprs) {
			FunpSeq f = new FunpSeq();
			f.exprs = exprs;
			return f;
		}
	}

}
