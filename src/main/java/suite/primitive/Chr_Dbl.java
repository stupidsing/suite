package suite.primitive;

import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.streamlet.ChrOutlet;
import suite.primitive.streamlet.DblStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Chr_Dbl {

	public double apply(char c);

	public static Fun<ChrOutlet, DblStreamlet> lift(Chr_Dbl fun0) {
		Chr_Dbl fun1 = fun0.rethrow();
		return ts -> {
			DoublesBuilder b = new DoublesBuilder();
			char t;
			while ((t = ts.next()) != ChrFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toDoubles().streamlet();
		};
	}

	public default Chr_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
