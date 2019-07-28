package suite.primitive;

import static suite.util.Fail.fail;

import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.streamlet.IntPuller;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.FunUtil.Fun;

public interface Int_Int {

	public int apply(int c);

	public static Fun<IntPuller, IntStreamlet> lift(Int_Int fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new IntsBuilder();
			int c;
			while ((c = ts.pull()) != IntFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toInts().streamlet();
		};
	}

	public static Obj_Int<IntPuller> sum(Int_Int fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			int c;
			var result = (int) 0;
			while ((c = source.g()) != IntFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Int_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
