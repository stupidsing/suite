package suite.primitive;

import suite.primitive.Ints.IntsBuilder;
import suite.primitive.streamlet.IntOutlet;
import suite.primitive.streamlet.IntStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Int_Int {

	public int apply(int c);

	public static Fun<IntOutlet, IntStreamlet> lift(Int_Int fun0) {
		Int_Int fun1 = fun0.rethrow();
		return ts -> {
			IntsBuilder b = new IntsBuilder();
			int t;
			while ((t = ts.next()) != IntFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toInts().streamlet();
		};
	}

	public default Int_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
