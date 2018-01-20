package suite.primitive;

import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.LngPrimitives.LngSource;
import suite.primitive.streamlet.FltStreamlet;
import suite.primitive.streamlet.LngOutlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Lng_Flt {

	public float apply(long c);

	public static Fun<LngOutlet, FltStreamlet> lift(Lng_Flt fun0) {
		Lng_Flt fun1 = fun0.rethrow();
		return ts -> {
			FloatsBuilder b = new FloatsBuilder();
			long c;
			while ((c = ts.next()) != LngFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toFloats().streamlet();
		};
	}

	public static Obj_Flt<LngOutlet> sum(Lng_Flt fun0) {
		Lng_Flt fun1 = fun0.rethrow();
		return outlet -> {
			LngSource source = outlet.source();
			long c;
			float result = (float) 0;
			while ((c = source.source()) != LngFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Lng_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return Fail.t("for key " + t, ex);
			}
		};
	}

}
