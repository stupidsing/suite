package suite.primitive;

import suite.primitive.Longs.LongsBuilder;
import suite.primitive.streamlet.ChrOutlet;
import suite.primitive.streamlet.LngStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Chr_Lng {

	public long apply(char c);

	public static Fun<ChrOutlet, LngStreamlet> lift(Chr_Lng fun0) {
		Chr_Lng fun1 = fun0.rethrow();
		return ts -> {
			LongsBuilder b = new LongsBuilder();
			char t;
			while ((t = ts.next()) != ChrFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toLongs().streamlet();
		};
	}

	public default Chr_Lng rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
