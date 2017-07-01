package suite.primitive;

import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.FltPrimitives.FltSource;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.streamlet.FltOutlet;
import suite.primitive.streamlet.FltStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Flt_Flt {

	public float apply(float c);

	public static Fun<FltOutlet, FltStreamlet> lift(Flt_Flt fun0) {
		Flt_Flt fun1 = fun0.rethrow();
		return ts -> {
			FloatsBuilder b = new FloatsBuilder();
			float c;
			while ((c = ts.next()) != FltFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toFloats().streamlet();
		};
	}

	public static Obj_Flt<FltOutlet> sum(Flt_Flt fun0) {
		Flt_Flt fun1 = fun0.rethrow();
		return outlet -> {
			FltSource source = outlet.source();
			float c;
			float result = (float) 0;
			while ((c = source.source()) != FltFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Flt_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
