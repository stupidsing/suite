package suite.primitive;

import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.streamlet.FltStreamlet;
import suite.primitive.streamlet.LngOutlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Lng_Flt {

	public float apply(long c);

	public static Fun<LngOutlet, FltStreamlet> lift(Lng_Flt fun0) {
		Lng_Flt fun1 = fun0.rethrow();
		return ts -> {
			FloatsBuilder b = new FloatsBuilder();
			long t;
			while ((t = ts.next()) != LngFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toFloats().streamlet();
		};
	}

	public default Lng_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
