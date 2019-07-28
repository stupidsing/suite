package suite.primitive;

import static suite.util.Fail.fail;

import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.streamlet.DblStreamlet;
import suite.primitive.streamlet.FltPuller;
import suite.streamlet.FunUtil.Fun;

public interface Flt_Dbl {

	public double apply(float c);

	public static Fun<FltPuller, DblStreamlet> lift(Flt_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new DoublesBuilder();
			float c;
			while ((c = ts.pull()) != FltFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toDoubles().streamlet();
		};
	}

	public static Obj_Dbl<FltPuller> sum(Flt_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			float c;
			var result = (double) 0;
			while ((c = source.g()) != FltFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Flt_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
