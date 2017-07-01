package suite.primitive;

import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.streamlet.DblOutlet;
import suite.primitive.streamlet.FltStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Dbl_Flt {

	public float apply(double c);

	public static Fun<DblOutlet, FltStreamlet> lift(Dbl_Flt fun0) {
		Dbl_Flt fun1 = fun0.rethrow();
		return ts -> {
			FloatsBuilder b = new FloatsBuilder();
			double c;
			while ((c = ts.next()) != DblFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toFloats().streamlet();
		};
	}

	public static Obj_Flt<DblOutlet> sum(Dbl_Flt fun0) {
		Dbl_Flt fun1 = fun0.rethrow();
		return outlet -> {
			DblSource source = outlet.source();
			double c;
			float result = (float) 0;
			while ((c = source.source()) != DblFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Dbl_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
