package suite.primitive;

import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.LngPrimitives.LngSource;
import suite.primitive.streamlet.DblStreamlet;
import suite.primitive.streamlet.LngOutlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Lng_Dbl {

	public double apply(long c);

	public static Fun<LngOutlet, DblStreamlet> lift(Lng_Dbl fun0) {
		Lng_Dbl fun1 = fun0.rethrow();
		return ts -> {
			DoublesBuilder b = new DoublesBuilder();
			long c;
			while ((c = ts.next()) != LngFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toDoubles().streamlet();
		};
	}

	public static Obj_Dbl<LngOutlet> sum(Lng_Dbl fun0) {
		Lng_Dbl fun1 = fun0.rethrow();
		return outlet -> {
			LngSource source = outlet.source();
			long c;
			var result = (double) 0;
			while ((c = source.source()) != LngFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Lng_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return Fail.t("for key " + t, ex);
			}
		};
	}

}
