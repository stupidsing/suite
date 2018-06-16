package suite.primitive;

import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.streamlet.ChrOutlet;
import suite.primitive.streamlet.DblStreamlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

public interface Chr_Dbl {

	public double apply(char c);

	public static Fun<ChrOutlet, DblStreamlet> lift(Chr_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new DoublesBuilder();
			char c;
			while ((c = ts.next()) != ChrFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toDoubles().streamlet();
		};
	}

	public static Obj_Dbl<ChrOutlet> sum(Chr_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return outlet -> {
			var source = outlet.source();
			char c;
			var result = (double) 0;
			while ((c = source.source()) != ChrFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Chr_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return Fail.t("for key " + t, ex);
			}
		};
	}

}
