package suite.primitive;

import static primal.statics.Fail.fail;

import primal.fp.Funs.Fun;
import primal.primitive.IntPrim;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.streamlet.DblStreamlet;
import suite.primitive.streamlet.IntPuller;

public interface Int_Dbl {

	public double apply(int c);

	public static Fun<IntPuller, DblStreamlet> lift(Int_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new DoublesBuilder();
			int c;
			while ((c = ts.pull()) != IntPrim.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toDoubles().streamlet();
		};
	}

	public static Obj_Dbl<IntPuller> sum(Int_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			int c;
			var result = (double) 0;
			while ((c = source.g()) != IntPrim.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Int_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
