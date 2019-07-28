package suite.primitive;

import static suite.util.Fail.fail;

import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.streamlet.IntStreamlet;
import suite.primitive.streamlet.LngPuller;
import suite.streamlet.FunUtil.Fun;

public interface Lng_Int {

	public int apply(long c);

	public static Fun<LngPuller, IntStreamlet> lift(Lng_Int fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new IntsBuilder();
			long c;
			while ((c = ts.pull()) != LngFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toInts().streamlet();
		};
	}

	public static Obj_Int<LngPuller> sum(Lng_Int fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			long c;
			var result = (int) 0;
			while ((c = source.g()) != LngFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Lng_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
