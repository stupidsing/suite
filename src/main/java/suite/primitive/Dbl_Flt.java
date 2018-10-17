package suite.primitive;

import static suite.util.Friends.fail;

import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.streamlet.DblOutlet;
import suite.primitive.streamlet.FltStreamlet;
import suite.streamlet.FunUtil.Fun;

public interface Dbl_Flt {

	public float apply(double c);

	public static Fun<DblOutlet, FltStreamlet> lift(Dbl_Flt fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new FloatsBuilder();
			double c;
			while ((c = ts.next()) != DblFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toFloats().streamlet();
		};
	}

	public static Obj_Flt<DblOutlet> sum(Dbl_Flt fun0) {
		var fun1 = fun0.rethrow();
		return outlet -> {
			var source = outlet.source();
			double c;
			var result = (float) 0;
			while ((c = source.g()) != DblFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Dbl_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
