package suite.primitive;

import static suite.util.Fail.fail;

import suite.primitive.Chars.CharsBuilder;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.streamlet.ChrStreamlet;
import suite.primitive.streamlet.DblPuller;
import suite.streamlet.FunUtil.Fun;

public interface Dbl_Chr {

	public char apply(double c);

	public static Fun<DblPuller, ChrStreamlet> lift(Dbl_Chr fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new CharsBuilder();
			double c;
			while ((c = ts.pull()) != DblFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toChars().streamlet();
		};
	}

	public static Obj_Chr<DblPuller> sum(Dbl_Chr fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			double c;
			var result = (char) 0;
			while ((c = source.g()) != DblFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Dbl_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
