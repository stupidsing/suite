package suite.primitive;

import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.streamlet.DblStreamlet;
import suite.primitive.streamlet.FltOutlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Flt_Dbl {

	public double apply(float c);

	public static Fun<FltOutlet, DblStreamlet> lift(Flt_Dbl fun0) {
		Flt_Dbl fun1 = fun0.rethrow();
		return ts -> {
			DoublesBuilder b = new DoublesBuilder();
			float t;
			while ((t = ts.next()) != FltFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toDoubles().streamlet();
		};
	}

	public default Flt_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
