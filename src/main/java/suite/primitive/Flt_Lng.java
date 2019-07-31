package suite.primitive;

import static primal.statics.Fail.fail;

import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.streamlet.FltPuller;
import suite.primitive.streamlet.LngStreamlet;
import suite.streamlet.FunUtil.Fun;

public interface Flt_Lng {

	public long apply(float c);

	public static Fun<FltPuller, LngStreamlet> lift(Flt_Lng fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new LongsBuilder();
			float c;
			while ((c = ts.pull()) != FltFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toLongs().streamlet();
		};
	}

	public static Obj_Lng<FltPuller> sum(Flt_Lng fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			float c;
			var result = (long) 0;
			while ((c = source.g()) != FltFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Flt_Lng rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
