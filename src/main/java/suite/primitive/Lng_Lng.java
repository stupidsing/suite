package suite.primitive;

import static suite.util.Friends.fail;

import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.streamlet.LngOutlet;
import suite.primitive.streamlet.LngStreamlet;
import suite.streamlet.FunUtil.Fun;

public interface Lng_Lng {

	public long apply(long c);

	public static Fun<LngOutlet, LngStreamlet> lift(Lng_Lng fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new LongsBuilder();
			long c;
			while ((c = ts.next()) != LngFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toLongs().streamlet();
		};
	}

	public static Obj_Lng<LngOutlet> sum(Lng_Lng fun0) {
		var fun1 = fun0.rethrow();
		return outlet -> {
			var source = outlet.source();
			long c;
			var result = (long) 0;
			while ((c = source.g()) != LngFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Lng_Lng rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
