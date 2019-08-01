package suite.primitive;

import static primal.statics.Fail.fail;

import primal.fp.Funs.Fun;
import primal.primitive.ChrPrim;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.streamlet.ChrPuller;
import suite.primitive.streamlet.IntStreamlet;

public interface Chr_Int {

	public int apply(char c);

	public static Fun<ChrPuller, IntStreamlet> lift(Chr_Int fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new IntsBuilder();
			char c;
			while ((c = ts.pull()) != ChrPrim.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toInts().streamlet();
		};
	}

	public static Obj_Int<ChrPuller> sum(Chr_Int fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			char c;
			var result = (int) 0;
			while ((c = source.g()) != ChrPrim.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Chr_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
