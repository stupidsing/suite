package suite.primitive; import static suite.util.Friends.fail;

import suite.primitive.Chars.CharsBuilder;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.streamlet.ChrOutlet;
import suite.primitive.streamlet.ChrStreamlet;
import suite.streamlet.FunUtil.Fun;

public interface Chr_Chr {

	public char apply(char c);

	public static Fun<ChrOutlet, ChrStreamlet> lift(Chr_Chr fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new CharsBuilder();
			char c;
			while ((c = ts.next()) != ChrFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toChars().streamlet();
		};
	}

	public static Obj_Chr<ChrOutlet> sum(Chr_Chr fun0) {
		var fun1 = fun0.rethrow();
		return outlet -> {
			var source = outlet.source();
			char c;
			var result = (char) 0;
			while ((c = source.source()) != ChrFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Chr_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
