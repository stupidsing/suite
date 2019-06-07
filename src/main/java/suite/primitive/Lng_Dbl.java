package suite.primitive;

import static suite.util.Friends.fail;

import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.streamlet.DblStreamlet;
import suite.primitive.streamlet.LngPuller;
import suite.streamlet.FunUtil.Fun;

public interface Lng_Dbl {

	public double apply(long c);

	public static Fun<LngPuller, DblStreamlet> lift(Lng_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new DoublesBuilder();
			long c;
			while ((c = ts.pull()) != LngFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toDoubles().streamlet();
		};
	}

	public static Obj_Dbl<LngPuller> sum(Lng_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			long c;
			var result = (double) 0;
			while ((c = source.g()) != LngFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Lng_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
