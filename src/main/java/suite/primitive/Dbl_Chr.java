package suite.primitive;

import suite.primitive.Chars.CharsBuilder;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.DblPrimitives.DblSource;
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
			double c;
			while ((c = ts.next()) != DblFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toChars().streamlet();
		};
	}

	public static Obj_Chr<DblOutlet> sum(Dbl_Chr fun0) {
		Dbl_Chr fun1 = fun0.rethrow();
		return outlet -> {
			DblSource source = outlet.source();
			double c;
			char result = (char) 0;
			while ((c = source.source()) != DblFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
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
