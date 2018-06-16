package suite.primitive;

import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.streamlet.ChrOutlet;
import suite.primitive.streamlet.LngStreamlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

public interface Chr_Lng {

	public long apply(char c);

	public static Fun<ChrOutlet, LngStreamlet> lift(Chr_Lng fun0) {
		var fun1 = fun0.rethrow();
		return ts -> {
			var b = new LongsBuilder();
			char c;
			while ((c = ts.next()) != ChrFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toLongs().streamlet();
		};
	}

	public static Obj_Lng<ChrOutlet> sum(Chr_Lng fun0) {
		var fun1 = fun0.rethrow();
		return outlet -> {
			var source = outlet.source();
			char c;
			var result = (long) 0;
			while ((c = source.source()) != ChrFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Chr_Lng rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				return Fail.t("for key " + t, ex);
			}
		};
	}

}
