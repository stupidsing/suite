package suite.primitive;

import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.streamlet.DblOutlet;
import suite.primitive.streamlet.DblStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Dbl_Dbl {

	public double apply(double c);

	public static Fun<DblOutlet, DblStreamlet> lift(Dbl_Dbl fun0) {
		Dbl_Dbl fun1 = fun0.rethrow();
		return ts -> {
			DoublesBuilder b = new DoublesBuilder();
			double t;
			while ((t = ts.next()) != DblFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toDoubles().streamlet();
		};
	}

	public default Dbl_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
