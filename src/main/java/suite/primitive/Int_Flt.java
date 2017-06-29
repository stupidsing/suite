package suite.primitive;

import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.streamlet.FltStreamlet;
import suite.primitive.streamlet.IntOutlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Int_Flt {

	public float apply(int c);

	public static Fun<IntOutlet, FltStreamlet> lift(Int_Flt fun0) {
		Int_Flt fun1 = fun0.rethrow();
		return ts -> {
			FloatsBuilder b = new FloatsBuilder();
			int t;
			while ((t = ts.next()) != IntFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toFloats().streamlet();
		};
	}

	public default Int_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
