package suite.funp;

import suite.funp.Funp_.Funp;
import suite.util.FunUtil.Fun;

public class P1 {

	public static class FunpAllocStack implements Funp {
		public int size; // allocate size
		public Fun<FunpMemory, Funp> expr;

		public FunpAllocStack(int size, Fun<FunpMemory, Funp> expr) {
			this.size = size;
			this.expr = expr;
		}
	}

	public static class FunpAssign implements Funp {
		public FunpMemory memory;
		public Funp value;

		public FunpAssign(FunpMemory memory, Funp value) {
			this.memory = memory;
			this.value = value;
		}
	}

	public static class FunpFramePointer implements Funp {
	}

	public static class FunpInvoke implements Funp {
		public Funp lambda;

		public FunpInvoke(Funp lambda) {
			this.lambda = lambda;
		}
	}

	public static class FunpMemory implements Funp {
		public Funp pointer;
		public int start;
		public int end;

		public FunpMemory(Funp pointer, int start, int end) {
			this.pointer = pointer;
			this.start = start;
			this.end = end;
		}

		public FunpMemory range(int s, int e) {
			return new FunpMemory(pointer, start + s, start + e);
		}

		public int size() {
			return end - start;
		}
	}

	public static class FunpSaveEbp implements Funp {
		public Funp expr;

		public FunpSaveEbp(Funp expr) {
			this.expr = expr;
		}
	}

	public static class FunpSaveRegisters implements Funp {
		public Funp expr;

		public FunpSaveRegisters(Funp expr) {
			this.expr = expr;
		}
	}

	public static class FunpSeq implements Funp {
		public Funp[] exprs;

		public FunpSeq(Funp... exprs) {
			this.exprs = exprs;
		}
	}

}
