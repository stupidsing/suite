package suite.primitive;

import static primal.statics.Fail.fail;

import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.streamlet.DblPuller;
import suite.primitive.streamlet.LngStreamlet;
import suite.streamlet.FunUtil.Fun;

public interface Dbl_Lng {

	public long apply(double c);

	public static Fun<DblPuller, LngStreamlet> lift(Dbl_Lng fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new LongsBuilder();
			double c;
			while ((c = ts.pull()) != DblFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toLongs().streamlet();
		};
	}

	public static Obj_Lng<DblPuller> sum(Dbl_Lng fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			double c;
			var result = (long) 0;
			while ((c = source.g()) != DblFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Dbl_Lng rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
