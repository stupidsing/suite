package suite.primitive;

import suite.primitive.Chars.CharsBuilder;
import suite.primitive.streamlet.ChrOutlet;
import suite.primitive.streamlet.ChrStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Chr_Chr {

	public char apply(char c);

	public static Fun<ChrOutlet, ChrStreamlet> lift(Chr_Chr fun0) {
		Chr_Chr fun1 = fun0.rethrow();
		return ts -> {
			CharsBuilder b = new CharsBuilder();
			char t;
			while ((t = ts.next()) != ChrFunUtil.EMPTYVALUE)
				b.append(fun1.apply(t));
			return b.toChars().streamlet();
		};
	}

	public default Chr_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
