package suite.primitive;

import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.streamlet.FltOutlet;
import suite.primitive.streamlet.FltStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Flt_Flt {

	public float apply(float c);

	public static Fun<FltOutlet, FltStreamlet> lift(Flt_Flt fun0) {
		Flt_Flt fun1 = fun0.rethrow();
		return ts -> {
			FloatsBuilder b = new FloatsBuilder();
			float t;
			while ((t = ts.next()) != FltFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toFloats().streamlet();
		};
	}

	public default Flt_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
