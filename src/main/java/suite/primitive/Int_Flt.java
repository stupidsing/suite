package suite.primitive;

import static primal.statics.Fail.fail;

import primal.fp.Funs.Fun;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.streamlet.FltStreamlet;
import suite.primitive.streamlet.IntPuller;

public interface Int_Flt {

	public float apply(int c);

	public static Fun<IntPuller, FltStreamlet> lift(Int_Flt fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new FloatsBuilder();
			int c;
			while ((c = ts.pull()) != IntFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toFloats().streamlet();
		};
	}

	public static Obj_Flt<IntPuller> sum(Int_Flt fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			int c;
			var result = (float) 0;
			while ((c = source.g()) != IntFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Int_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
