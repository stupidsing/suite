package suite.primitive;

import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.streamlet.DblStreamlet;
import suite.primitive.streamlet.LngOutlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Lng_Dbl {

	public double apply(long c);

	public static Fun<LngOutlet, DblStreamlet> lift(Lng_Dbl fun0) {
		Lng_Dbl fun1 = fun0.rethrow();
		return ts -> {
			DoublesBuilder b = new DoublesBuilder();
			long t;
			while ((t = ts.next()) != LngFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toDoubles().streamlet();
		};
	}

	public default Lng_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
