package suite.funp;

import suite.funp.Funp_.PN0;
import suite.funp.Funp_.PN1;

public class P1 {

	public static class FunpFramePointer implements PN1 {
		public final int scope;

		public FunpFramePointer(int scope) {
			this.scope = scope;
		}
	}

	public static class FunpMemory implements PN1 {
		public final PN0 pointer;
		public final int start;
		public final int end;

		public FunpMemory(PN0 pointer, int start, int end) {
			this.pointer = pointer;
			this.start = start;
			this.end = end;
		}
	}

}
