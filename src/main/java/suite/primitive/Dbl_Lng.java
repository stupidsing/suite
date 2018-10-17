package suite.primitive;

import static suite.util.Friends.fail;

import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.streamlet.DblOutlet;
import suite.primitive.streamlet.LngStreamlet;
import suite.streamlet.FunUtil.Fun;

public interface Dbl_Lng {

	public long apply(double c);

	public static Fun<DblOutlet, LngStreamlet> lift(Dbl_Lng fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new LongsBuilder();
			double c;
			while ((c = ts.next()) != DblFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toLongs().streamlet();
		};
	}

	public static Obj_Lng<DblOutlet> sum(Dbl_Lng fun0) {
		var fun1 = fun0.rethrow();
		return outlet -> {
			var source = outlet.source();
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
