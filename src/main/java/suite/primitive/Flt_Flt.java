package suite.primitive;

import static primal.statics.Fail.fail;

import primal.fp.Funs.Fun;
import primal.primitive.FltPrim;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.streamlet.FltPuller;
import suite.primitive.streamlet.FltStreamlet;

public interface Flt_Flt {

	public float apply(float c);

	public static Fun<FltPuller, FltStreamlet> lift(Flt_Flt fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new FloatsBuilder();
			float c;
			while ((c = ts.pull()) != FltPrim.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toFloats().streamlet();
		};
	}

	public static Obj_Flt<FltPuller> sum(Flt_Flt fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			float c;
			var result = (float) 0;
			while ((c = source.g()) != FltPrim.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Flt_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
