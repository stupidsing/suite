package suite.primitive;

import static primal.statics.Fail.fail;

import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.streamlet.DblPuller;
import suite.primitive.streamlet.DblStreamlet;
import suite.streamlet.FunUtil.Fun;

public interface Dbl_Dbl {

	public double apply(double c);

	public static Fun<DblPuller, DblStreamlet> lift(Dbl_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new DoublesBuilder();
			double c;
			while ((c = ts.pull()) != DblFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toDoubles().streamlet();
		};
	}

	public static Obj_Dbl<DblPuller> sum(Dbl_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			double c;
			var result = (double) 0;
			while ((c = source.g()) != DblFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Dbl_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
