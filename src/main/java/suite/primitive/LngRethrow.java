package suite.primitive;

import suite.primitive.LngFun.LngObj_Obj;
import suite.primitive.LngPredicate.LngObjPredicate;
import suite.primitive.LngPredicate.LngPredicate_;

public class LngRethrow {

	public static <T> LngObjPredicate<T> lngObjPredicate(LngObjPredicate<T> predicate) {
		return (c, t) -> {
			try {
				return predicate.test(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

	public static <X, Y> LngObj_Obj<X, Y> fun2(LngObj_Obj<X, Y> predicate) {
		return (x, y) -> {
			try {
				return predicate.apply(x, y);
			} catch (Exception ex) {
				throw new RuntimeException("for " + x + ":" + y + ", ", ex);
			}
		};
	}

	public static LngPredicate_ predicate(LngPredicate_ predicate) {
		return c -> {
			try {
				return predicate.test(c);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", ", ex);
			}
		};
	}

}
