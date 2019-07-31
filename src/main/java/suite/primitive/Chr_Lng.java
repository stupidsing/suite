package suite.primitive;

import static primal.statics.Fail.fail;

import primal.fp.Funs.Fun;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.streamlet.ChrPuller;
import suite.primitive.streamlet.LngStreamlet;

public interface Chr_Lng {

	public long apply(char c);

	public static Fun<ChrPuller, LngStreamlet> lift(Chr_Lng fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new LongsBuilder();
			char c;
			while ((c = ts.pull()) != ChrFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toLongs().streamlet();
		};
	}

	public static Obj_Lng<ChrPuller> sum(Chr_Lng fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			char c;
			var result = (long) 0;
			while ((c = source.g()) != ChrFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Chr_Lng rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
