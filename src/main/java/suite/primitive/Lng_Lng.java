package suite.primitive;

import suite.primitive.Longs.LongsBuilder;
import suite.primitive.streamlet.LngOutlet;
import suite.primitive.streamlet.LngStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Lng_Lng {

	public long apply(long c);

	public static Fun<LngOutlet, LngStreamlet> lift(Lng_Lng fun0) {
		Lng_Lng fun1 = fun0.rethrow();
		return ts -> {
			LongsBuilder b = new LongsBuilder();
			long t;
			while ((t = ts.next()) != LngFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toLongs().streamlet();
		};
	}

	public default Lng_Lng rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
