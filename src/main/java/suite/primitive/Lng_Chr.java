package suite.primitive;

import suite.primitive.Chars.CharsBuilder;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.LngPrimitives.LngSource;
import suite.primitive.streamlet.ChrStreamlet;
import suite.primitive.streamlet.LngOutlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Lng_Chr {

	public char apply(long c);

	public static Fun<LngOutlet, ChrStreamlet> lift(Lng_Chr fun0) {
		Lng_Chr fun1 = fun0.rethrow();
		return ts -> {
			CharsBuilder b = new CharsBuilder();
			long c;
			while ((c = ts.next()) != LngFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toChars().streamlet();
		};
	}

	public static Obj_Chr<LngOutlet> sum(Lng_Chr fun0) {
		Lng_Chr fun1 = fun0.rethrow();
		return outlet -> {
			LngSource source = outlet.source();
			long c;
			char result = (char) 0;
			while ((c = source.source()) != LngFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Lng_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return Fail.t("for key " + t, ex);
			}
		};
	}

}
