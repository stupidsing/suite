package suite.funp;

import suite.funp.Funp_.Funp;

public class P1 {

	public static class FunpAssign implements Funp {
		public FunpMemory memory;
		public Funp value;
		public Funp expr;

		public FunpAssign(FunpMemory memory, Funp value, Funp expr) {
			this.memory = memory;
			this.value = value;
			this.expr = expr;
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

	public static class FunpStack implements Funp {
		public int size; // allocate size
		public Funp expr;

		public FunpStack(int size, Funp expr) {
			this.size = size;
			this.expr = expr;
		}
	}

	public static class FunpStackPointer implements Funp {
	}

}
