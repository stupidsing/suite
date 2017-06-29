package suite.primitive;

import suite.primitive.Chars.CharsBuilder;
import suite.primitive.streamlet.ChrStreamlet;
import suite.primitive.streamlet.IntOutlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Int_Chr {

	public char apply(int c);

	public static Fun<IntOutlet, ChrStreamlet> lift(Int_Chr fun0) {
		Int_Chr fun1 = fun0.rethrow();
		return ts -> {
			CharsBuilder b = new CharsBuilder();
			int t;
			while ((t = ts.next()) != IntFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toChars().streamlet();
		};
	}

	public default Int_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
