package suite.primitive;

import static primal.statics.Fail.fail;

import primal.fp.Funs.Fun;
import primal.primitive.ChrPrim;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.streamlet.ChrPuller;
import suite.primitive.streamlet.FltStreamlet;

public interface Chr_Flt {

	public float apply(char c);

	public static Fun<ChrPuller, FltStreamlet> lift(Chr_Flt fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new FloatsBuilder();
			char c;
			while ((c = ts.pull()) != ChrPrim.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toFloats().streamlet();
		};
	}

	public static Obj_Flt<ChrPuller> sum(Chr_Flt fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			char c;
			var result = (float) 0;
			while ((c = source.g()) != ChrPrim.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Chr_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
