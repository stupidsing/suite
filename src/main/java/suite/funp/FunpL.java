package suite.funp;

import suite.funp.Funp_.Funp;

public class FunpL {

	public class FunpFramePointer extends Funp {
		public final int scope;

		public FunpFramePointer(int scope) {
			this.scope = scope;
		}
	}

	public class FunpMemory extends Funp {
		public final Funp pointer;
		public final int start;
		public final int end;

		public FunpMemory(Funp pointer, int start, int end) {
			this.pointer = pointer;
			this.start = start;
			this.end = end;
		}
	}

}
