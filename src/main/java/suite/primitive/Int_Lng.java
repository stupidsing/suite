package suite.primitive;

import suite.primitive.IntPrimitives.IntSource;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.streamlet.IntOutlet;
import suite.primitive.streamlet.LngStreamlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Int_Lng {

	public long apply(int c);

	public static Fun<IntOutlet, LngStreamlet> lift(Int_Lng fun0) {
		Int_Lng fun1 = fun0.rethrow();
		return ts -> {
			LongsBuilder b = new LongsBuilder();
			int c;
			while ((c = ts.next()) != IntFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toLongs().streamlet();
		};
	}

	public static Obj_Lng<IntOutlet> sum(Int_Lng fun0) {
		Int_Lng fun1 = fun0.rethrow();
		return outlet -> {
			IntSource source = outlet.source();
			int c;
			var result = (long) 0;
			while ((c = source.source()) != IntFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Int_Lng rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return Fail.t("for key " + t, ex);
			}
		};
	}

}
