package suite.primitive;

import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.FltPrimitives.FltSource;
import suite.primitive.streamlet.DblStreamlet;
import suite.primitive.streamlet.FltOutlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Flt_Dbl {

	public double apply(float c);

	public static Fun<FltOutlet, DblStreamlet> lift(Flt_Dbl fun0) {
		Flt_Dbl fun1 = fun0.rethrow();
		return ts -> {
			DoublesBuilder b = new DoublesBuilder();
			float c;
			while ((c = ts.next()) != FltFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toDoubles().streamlet();
		};
	}

	public static Obj_Dbl<FltOutlet> sum(Flt_Dbl fun0) {
		Flt_Dbl fun1 = fun0.rethrow();
		return outlet -> {
			FltSource source = outlet.source();
			float c;
			double result = (double) 0;
			while ((c = source.source()) != FltFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Flt_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
