package suite.primitive;

import suite.primitive.FltPrimitiveFun.FltObj_Obj;
import suite.primitive.FltPrimitivePredicate.FltObjPredicate;
import suite.primitive.FltPrimitivePredicate.FltPredicate_;

public class FltRethrow {

	public static <T> FltObjPredicate<T> fltObjPredicate(FltObjPredicate<T> predicate) {
		return (c, t) -> {
			try {
				return predicate.test(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

	public static <X, Y> FltObj_Obj<X, Y> fun2(FltObj_Obj<X, Y> predicate) {
		return (x, y) -> {
			try {
				return predicate.apply(x, y);
			} catch (Exception ex) {
				throw new RuntimeException("for " + x + ":" + y + ", ", ex);
			}
		};
	}

	public static FltPredicate_ predicate(FltPredicate_ predicate) {
		return c -> {
			try {
				return predicate.test(c);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", ", ex);
			}
		};
	}

}
