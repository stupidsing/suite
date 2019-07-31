package suite.primitive;

import static primal.statics.Fail.fail;

import primal.fp.Funs.Fun;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.streamlet.DblPuller;
import suite.primitive.streamlet.FltStreamlet;

public interface Dbl_Flt {

	public float apply(double c);

	public static Fun<DblPuller, FltStreamlet> lift(Dbl_Flt fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new FloatsBuilder();
			double c;
			while ((c = ts.pull()) != DblFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toFloats().streamlet();
		};
	}

	public static Obj_Flt<DblPuller> sum(Dbl_Flt fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			double c;
			var result = (float) 0;
			while ((c = source.g()) != DblFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Dbl_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
