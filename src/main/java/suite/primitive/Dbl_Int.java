package suite.primitive;

import static suite.util.Friends.fail;

import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.streamlet.DblOutlet;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.FunUtil.Fun;

public interface Dbl_Int {

	public int apply(double c);

	public static Fun<DblOutlet, IntStreamlet> lift(Dbl_Int fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new IntsBuilder();
			double c;
			while ((c = ts.next()) != DblFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toInts().streamlet();
		};
	}

	public static Obj_Int<DblOutlet> sum(Dbl_Int fun0) {
		var fun1 = fun0.rethrow();
		return outlet -> {
			var source = outlet.source();
			double c;
			var result = (int) 0;
			while ((c = source.source()) != DblFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Dbl_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return fail("for key " + t, ex);
			}
		};
	}

}
