package suite.primitive;

import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.LngPrimitives.LngSource;
import suite.primitive.streamlet.IntStreamlet;
import suite.primitive.streamlet.LngOutlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Lng_Int {

	public int apply(long c);

	public static Fun<LngOutlet, IntStreamlet> lift(Lng_Int fun0) {
		Lng_Int fun1 = fun0.rethrow();
		return ts -> {
			IntsBuilder b = new IntsBuilder();
			long c;
			while ((c = ts.next()) != LngFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toInts().streamlet();
		};
	}

	public static Obj_Int<LngOutlet> sum(Lng_Int fun0) {
		Lng_Int fun1 = fun0.rethrow();
		return outlet -> {
			LngSource source = outlet.source();
			long c;
			var result = (int) 0;
			while ((c = source.source()) != LngFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Lng_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return Fail.t("for key " + t, ex);
			}
		};
	}

}
