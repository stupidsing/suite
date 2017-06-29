package suite.primitive;

import suite.primitive.Chars.CharsBuilder;
import suite.primitive.streamlet.ChrStreamlet;
import suite.primitive.streamlet.FltOutlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Flt_Chr {

	public char apply(float c);

	public static Fun<FltOutlet, ChrStreamlet> lift(Flt_Chr fun0) {
		Flt_Chr fun1 = fun0.rethrow();
		return ts -> {
			CharsBuilder b = new CharsBuilder();
			float t;
			while ((t = ts.next()) != FltFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toChars().streamlet();
		};
	}

	public default Flt_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
