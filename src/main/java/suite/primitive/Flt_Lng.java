package suite.primitive;

import suite.primitive.FltPrimitives.FltSource;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.streamlet.FltOutlet;
import suite.primitive.streamlet.LngStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Flt_Lng {

	public long apply(float c);

	public static Fun<FltOutlet, LngStreamlet> lift(Flt_Lng fun0) {
		Flt_Lng fun1 = fun0.rethrow();
		return ts -> {
			LongsBuilder b = new LongsBuilder();
			float c;
			while ((c = ts.next()) != FltFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toLongs().streamlet();
		};
	}

	public static Obj_Lng<FltOutlet> sum(Flt_Lng fun0) {
		Flt_Lng fun1 = fun0.rethrow();
		return outlet -> {
			FltSource source = outlet.source();
			float c;
			long result = (long) 0;
			while ((c = source.source()) != FltFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Flt_Lng rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
