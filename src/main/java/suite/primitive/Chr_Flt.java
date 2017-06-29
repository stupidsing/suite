package suite.primitive;

import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.streamlet.ChrOutlet;
import suite.primitive.streamlet.FltStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Chr_Flt {

	public float apply(char c);

	public static Fun<ChrOutlet, FltStreamlet> lift(Chr_Flt fun0) {
		Chr_Flt fun1 = fun0.rethrow();
		return ts -> {
			FloatsBuilder b = new FloatsBuilder();
			char t;
			while ((t = ts.next()) != ChrFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toFloats().streamlet();
		};
	}

	public default Chr_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
