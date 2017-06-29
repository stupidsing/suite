package suite.primitive;

import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.streamlet.DblStreamlet;
import suite.primitive.streamlet.IntOutlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Int_Dbl {

	public double apply(int c);

	public static Fun<IntOutlet, DblStreamlet> lift(Int_Dbl fun0) {
		Int_Dbl fun1 = fun0.rethrow();
		return ts -> {
			DoublesBuilder b = new DoublesBuilder();
			int t;
			while ((t = ts.next()) != IntFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toDoubles().streamlet();
		};
	}

	public default Int_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
