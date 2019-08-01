package suite.primitive;

import static primal.statics.Fail.fail;

import primal.fp.Funs.Fun;
import primal.primitive.ChrPrim;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.streamlet.ChrPuller;
import suite.primitive.streamlet.ChrStreamlet;

public interface Chr_Chr {

	public char apply(char c);

	public static Fun<ChrPuller, ChrStreamlet> lift(Chr_Chr fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new CharsBuilder();
			char c;
			while ((c = ts.pull()) != ChrPrim.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toChars().streamlet();
		};
	}

	public static Obj_Chr<ChrPuller> sum(Chr_Chr fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			char c;
			var result = (char) 0;
			while ((c = source.g()) != ChrPrim.EMPTYVALUE)
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
