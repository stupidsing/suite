package suite.primitive;

import suite.primitive.Ints.IntsBuilder;
import suite.primitive.streamlet.IntStreamlet;
import suite.primitive.streamlet.LngOutlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Lng_Int {

	public int apply(long c);

	public static Fun<LngOutlet, IntStreamlet> lift(Lng_Int fun0) {
		Lng_Int fun1 = fun0.rethrow();
		return ts -> {
			IntsBuilder b = new IntsBuilder();
			long t;
			while ((t = ts.next()) != LngFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toInts().streamlet();
		};
	}

	public default Lng_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
