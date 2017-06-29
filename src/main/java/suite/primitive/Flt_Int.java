package suite.primitive;

import suite.primitive.Ints.IntsBuilder;
import suite.primitive.streamlet.FltOutlet;
import suite.primitive.streamlet.IntStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Flt_Int {

	public int apply(float c);

	public static Fun<FltOutlet, IntStreamlet> lift(Flt_Int fun0) {
		Flt_Int fun1 = fun0.rethrow();
		return ts -> {
			IntsBuilder b = new IntsBuilder();
			float t;
			while ((t = ts.next()) != FltFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toInts().streamlet();
		};
	}

	public default Flt_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
