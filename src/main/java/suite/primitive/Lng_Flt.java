package suite.primitive;

import static suite.util.Friends.fail;

import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.streamlet.FltStreamlet;
import suite.primitive.streamlet.LngOutlet;
import suite.streamlet.FunUtil.Fun;

public interface Lng_Flt {

	public float apply(long c);

	public static Fun<LngOutlet, FltStreamlet> lift(Lng_Flt fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new FloatsBuilder();
			long c;
			while ((c = ts.next()) != LngFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toFloats().streamlet();
		};
	}

	public static Obj_Flt<LngOutlet> sum(Lng_Flt fun0) {
		var fun1 = fun0.rethrow();
		return outlet -> {
			var source = outlet.source();
			long c;
			var result = (float) 0;
			while ((c = source.source()) != LngFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Lng_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
