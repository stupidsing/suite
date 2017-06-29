package suite.primitive;

import suite.primitive.Longs.LongsBuilder;
import suite.primitive.streamlet.IntOutlet;
import suite.primitive.streamlet.LngStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Int_Lng {

	public long apply(int c);

	public static Fun<IntOutlet, LngStreamlet> lift(Int_Lng fun0) {
		Int_Lng fun1 = fun0.rethrow();
		return ts -> {
			LongsBuilder b = new LongsBuilder();
			int t;
			while ((t = ts.next()) != IntFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toLongs().streamlet();
		};
	}

	public default Int_Lng rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
