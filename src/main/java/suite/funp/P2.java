package suite.funp;

import suite.funp.Funp_.Funp;
import suite.funp.P1.FunpMemory;

public class P2 {

	public interface End {
	}

	public static class FunpInvokeMemory implements Funp, P2.End {
		public FunpMemory memory;

		public static FunpInvokeMemory of(FunpMemory memory) {
			FunpInvokeMemory f = new FunpInvokeMemory();
			f.memory = memory;
			return f;
		}
	}

}
