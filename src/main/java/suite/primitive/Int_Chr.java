package suite.primitive;

import static primal.statics.Fail.fail;

import primal.fp.Funs.Fun;
import primal.primitive.IntPrim;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.streamlet.ChrStreamlet;
import suite.primitive.streamlet.IntPuller;

public interface Int_Chr {

	public char apply(int c);

	public static Fun<IntPuller, ChrStreamlet> lift(Int_Chr fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new CharsBuilder();
			int c;
			while ((c = ts.pull()) != IntPrim.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toChars().streamlet();
		};
	}

	public static Obj_Chr<IntPuller> sum(Int_Chr fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			int c;
			var result = (char) 0;
			while ((c = source.g()) != IntPrim.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Int_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
