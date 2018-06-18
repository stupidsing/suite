package suite.funp;

import suite.adt.pair.Fixie_.FixieFun2;
import suite.funp.Funp_.Funp;

public class P1 {

	public static class FunpTco implements Funp, P2.End {
		public String var;
		public Funp tco;

		public static FunpTco of(String var, Funp tco) {
			var f = new FunpTco();
			f.var = var;
			f.tco = tco;
			return f;
		}

		public <R> R apply(FixieFun2<String, Funp, R> fun) {
			return fun.apply(var, tco);
		}
	}

}
