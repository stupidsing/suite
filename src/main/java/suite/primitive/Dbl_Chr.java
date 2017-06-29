package suite.primitive;

import suite.primitive.Chars.CharsBuilder;
import suite.primitive.streamlet.ChrStreamlet;
import suite.primitive.streamlet.DblOutlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Dbl_Chr {

	public char apply(double c);

	public static Fun<DblOutlet, ChrStreamlet> lift(Dbl_Chr fun0) {
		Dbl_Chr fun1 = fun0.rethrow();
		return ts -> {
			CharsBuilder b = new CharsBuilder();
			double t;
			while ((t = ts.next()) != DblFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toChars().streamlet();
		};
	}

	public default Dbl_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
