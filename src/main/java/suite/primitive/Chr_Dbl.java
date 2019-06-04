package suite.primitive;

import static suite.util.Friends.fail;

import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.streamlet.ChrPuller;
import suite.primitive.streamlet.DblStreamlet;
import suite.streamlet.FunUtil.Fun;

public interface Chr_Dbl {

	public double apply(char c);

	public static Fun<ChrPuller, DblStreamlet> lift(Chr_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new DoublesBuilder();
			char c;
			while ((c = ts.pull()) != ChrFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toDoubles().streamlet();
		};
	}

	public static Obj_Dbl<ChrPuller> sum(Chr_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			char c;
			var result = (double) 0;
			while ((c = source.g()) != ChrFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Chr_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
