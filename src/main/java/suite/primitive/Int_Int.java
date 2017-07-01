package suite.primitive;

import suite.primitive.IntPrimitives.IntSource;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.streamlet.IntOutlet;
import suite.primitive.streamlet.IntStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Int_Int {

	public int apply(int c);

	public static Fun<IntOutlet, IntStreamlet> lift(Int_Int fun0) {
		Int_Int fun1 = fun0.rethrow();
		return ts -> {
			IntsBuilder b = new IntsBuilder();
			int c;
			while ((c = ts.next()) != IntFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toInts().streamlet();
		};
	}

	public static Obj_Int<IntOutlet> sum(Int_Int fun0) {
		Int_Int fun1 = fun0.rethrow();
		return outlet -> {
			IntSource source = outlet.source();
			int c;
			int result = (int) 0;
			while ((c = source.source()) != IntFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Int_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
