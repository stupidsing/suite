package suite.primitive;

import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.streamlet.FltStreamlet;
import suite.primitive.streamlet.IntOutlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

public interface Int_Flt {

	public float apply(int c);

	public static Fun<IntOutlet, FltStreamlet> lift(Int_Flt fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new FloatsBuilder();
			int c;
			while ((c = ts.next()) != IntFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toFloats().streamlet();
		};
	}

	public static Obj_Flt<IntOutlet> sum(Int_Flt fun0) {
		var fun1 = fun0.rethrow();
		return outlet -> {
			var source = outlet.source();
			int c;
			var result = (float) 0;
			while ((c = source.source()) != IntFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Int_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return Fail.t("for key " + t, ex);
			}
		};
	}

}
