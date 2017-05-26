package suite.primitive;

import suite.primitive.ShtFun.ShtObj_Obj;
import suite.primitive.ShtPredicate.ShtObjPredicate;
import suite.primitive.ShtPredicate.ShtPredicate_;

public class ShtRethrow {

	public static <T> ShtObjPredicate<T> shtObjPredicate(ShtObjPredicate<T> predicate) {
		return (c, t) -> {
			try {
				return predicate.test(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

	public static <X, Y> ShtObj_Obj<X, Y> fun2(ShtObj_Obj<X, Y> predicate) {
		return (x, y) -> {
			try {
				return predicate.apply(x, y);
			} catch (Exception ex) {
				throw new RuntimeException("for " + x + ":" + y + ", ", ex);
			}
		};
	}

	public static ShtPredicate_ predicate(ShtPredicate_ predicate) {
		return c -> {
			try {
				return predicate.test(c);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", ", ex);
			}
		};
	}

}
