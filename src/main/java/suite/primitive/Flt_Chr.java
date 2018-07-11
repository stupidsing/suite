package suite.primitive; import static suite.util.Friends.fail;

import suite.primitive.Chars.CharsBuilder;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.streamlet.ChrStreamlet;
import suite.primitive.streamlet.FltOutlet;
import suite.streamlet.FunUtil.Fun;

public interface Flt_Chr {

	public char apply(float c);

	public static Fun<FltOutlet, ChrStreamlet> lift(Flt_Chr fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new CharsBuilder();
			float c;
			while ((c = ts.next()) != FltFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toChars().streamlet();
		};
	}

	public static Obj_Chr<FltOutlet> sum(Flt_Chr fun0) {
		var fun1 = fun0.rethrow();
		return outlet -> {
			var source = outlet.source();
			float c;
			var result = (char) 0;
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
				return fail("for key " + t, ex);
			}
		};
	}

}
