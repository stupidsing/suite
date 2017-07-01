package suite.primitive;

import suite.primitive.LngPrimitives.LngSource;
import suite.primitive.LngPrimitives.Obj_Lng;
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
			long c;
			while ((c = ts.next()) != LngFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toLongs().streamlet();
		};
	}

	public static Obj_Lng<LngOutlet> sum(Lng_Lng fun0) {
		Lng_Lng fun1 = fun0.rethrow();
		return outlet -> {
			LngSource source = outlet.source();
			long c;
			long result = (long) 0;
			while ((c = source.source()) != LngFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
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
