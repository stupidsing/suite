package suite.primitive;

import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.streamlet.DblStreamlet;
import suite.primitive.streamlet.IntOutlet;
import suite.streamlet.FunUtil.Fun;
import suite.util.Fail;

public interface Int_Dbl {

	public double apply(int c);

	public static Fun<IntOutlet, DblStreamlet> lift(Int_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new DoublesBuilder();
			int c;
			while ((c = ts.next()) != IntFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toDoubles().streamlet();
		};
	}

	public static Obj_Dbl<IntOutlet> sum(Int_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return outlet -> {
			var source = outlet.source();
			int c;
			var result = (double) 0;
			while ((c = source.source()) != IntFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Int_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return Fail.t("for key " + t, ex);
			}
		};
	}

}
