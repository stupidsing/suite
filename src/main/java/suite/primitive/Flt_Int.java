package suite.primitive;

import static primal.statics.Fail.fail;

import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.streamlet.FltPuller;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.FunUtil.Fun;

public interface Flt_Int {

	public int apply(float c);

	public static Fun<FltPuller, IntStreamlet> lift(Flt_Int fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new IntsBuilder();
			float c;
			while ((c = ts.pull()) != FltFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toInts().streamlet();
		};
	}

	public static Obj_Int<FltPuller> sum(Flt_Int fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			float c;
			var result = (int) 0;
			while ((c = source.g()) != FltFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Flt_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
