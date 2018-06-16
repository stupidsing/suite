package suite.primitive;

import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.streamlet.DblOutlet;
import suite.primitive.streamlet.DblStreamlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

public interface Dbl_Dbl {

	public double apply(double c);

	public static Fun<DblOutlet, DblStreamlet> lift(Dbl_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new DoublesBuilder();
			double c;
			while ((c = ts.next()) != DblFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toDoubles().streamlet();
		};
	}

	public static Obj_Dbl<DblOutlet> sum(Dbl_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return outlet -> {
			var source = outlet.source();
			double c;
			var result = (double) 0;
			while ((c = source.source()) != DblFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Dbl_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return Fail.t("for key " + t, ex);
			}
		};
	}

}
