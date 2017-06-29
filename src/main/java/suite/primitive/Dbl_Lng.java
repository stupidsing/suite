package suite.primitive;

import suite.primitive.Longs.LongsBuilder;
import suite.primitive.streamlet.DblOutlet;
import suite.primitive.streamlet.LngStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Dbl_Lng {

	public long apply(double c);

	public static Fun<DblOutlet, LngStreamlet> lift(Dbl_Lng fun0) {
		Dbl_Lng fun1 = fun0.rethrow();
		return ts -> {
			LongsBuilder b = new LongsBuilder();
			double t;
			while ((t = ts.next()) != DblFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toLongs().streamlet();
		};
	}

	public default Dbl_Lng rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
