package suite.primitive;

import suite.primitive.Chars.CharsBuilder;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.FltPrimitives.FltSource;
import suite.primitive.streamlet.ChrStreamlet;
import suite.primitive.streamlet.FltOutlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Flt_Chr {

	public char apply(float c);

	public static Fun<FltOutlet, ChrStreamlet> lift(Flt_Chr fun0) {
		Flt_Chr fun1 = fun0.rethrow();
		return ts -> {
			CharsBuilder b = new CharsBuilder();
			float c;
			while ((c = ts.next()) != FltFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toChars().streamlet();
		};
	}

	public static Obj_Chr<FltOutlet> sum(Flt_Chr fun0) {
		Flt_Chr fun1 = fun0.rethrow();
		return outlet -> {
			FltSource source = outlet.source();
			float c;
			char result = (char) 0;
			while ((c = source.source()) != FltFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Flt_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return Fail.t("for key " + t, ex);
			}
		};
	}

}
