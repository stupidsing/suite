package suite.primitive;

import suite.primitive.Ints.IntsBuilder;
import suite.primitive.streamlet.DblOutlet;
import suite.primitive.streamlet.IntStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Dbl_Int {

	public int apply(double c);

	public static Fun<DblOutlet, IntStreamlet> lift(Dbl_Int fun0) {
		Dbl_Int fun1 = fun0.rethrow();
		return ts -> {
			IntsBuilder b = new IntsBuilder();
			double t;
			while ((t = ts.next()) != DblFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toInts().streamlet();
		};
	}

	public default Dbl_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
