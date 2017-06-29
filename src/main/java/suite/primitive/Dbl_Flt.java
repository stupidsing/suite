package suite.primitive;

import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.streamlet.DblOutlet;
import suite.primitive.streamlet.FltStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Dbl_Flt {

	public float apply(double c);

	public static Fun<DblOutlet, FltStreamlet> lift(Dbl_Flt fun0) {
		Dbl_Flt fun1 = fun0.rethrow();
		return ts -> {
			FloatsBuilder b = new FloatsBuilder();
			double t;
			while ((t = ts.next()) != DblFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toFloats().streamlet();
		};
	}

	public default Dbl_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
