package suite.primitive;

import suite.primitive.Ints.IntsBuilder;
import suite.primitive.streamlet.ChrOutlet;
import suite.primitive.streamlet.IntStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Chr_Int {

	public int apply(char c);

	public static Fun<ChrOutlet, IntStreamlet> lift(Chr_Int fun0) {
		Chr_Int fun1 = fun0.rethrow();
		return ts -> {
			IntsBuilder b = new IntsBuilder();
			char t;
			while ((t = ts.next()) != ChrFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toInts().streamlet();
		};
	}

	public default Chr_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
