package suite.primitive;

import static primal.statics.Fail.fail;

import primal.fp.Funs.Fun;
import primal.primitive.LngPrim;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.streamlet.ChrStreamlet;
import suite.primitive.streamlet.LngPuller;

public interface Lng_Chr {

	public char apply(long c);

	public static Fun<LngPuller, ChrStreamlet> lift(Lng_Chr fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new CharsBuilder();
			long c;
			while ((c = ts.pull()) != LngPrim.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toChars().streamlet();
		};
	}

	public static Obj_Chr<LngPuller> sum(Lng_Chr fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			long c;
			var result = (char) 0;
			while ((c = source.g()) != LngPrim.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Lng_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
