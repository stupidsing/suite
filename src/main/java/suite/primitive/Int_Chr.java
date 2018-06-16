package suite.primitive;

import suite.primitive.Chars.CharsBuilder;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.streamlet.ChrStreamlet;
import suite.primitive.streamlet.IntOutlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

public interface Int_Chr {

	public char apply(int c);

	public static Fun<IntOutlet, ChrStreamlet> lift(Int_Chr fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new CharsBuilder();
			int c;
			while ((c = ts.next()) != IntFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toChars().streamlet();
		};
	}

	public static Obj_Chr<IntOutlet> sum(Int_Chr fun0) {
		var fun1 = fun0.rethrow();
		return outlet -> {
			var source = outlet.source();
			int c;
			var result = (char) 0;
			while ((c = source.source()) != IntFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Int_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return Fail.t("for key " + t, ex);
			}
		};
	}

}
