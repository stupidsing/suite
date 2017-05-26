package suite.primitive;

import suite.primitive.IntFun.IntObj_Obj;
import suite.primitive.IntPredicate.IntObjPredicate;
import suite.primitive.IntPredicate.IntPredicate_;

public class IntRethrow {

	public static <T> IntObjPredicate<T> intObjPredicate(IntObjPredicate<T> predicate) {
		return (c, t) -> {
			try {
				return predicate.test(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

	public static <X, Y> IntObj_Obj<X, Y> fun2(IntObj_Obj<X, Y> predicate) {
		return (x, y) -> {
			try {
				return predicate.apply(x, y);
			} catch (Exception ex) {
				throw new RuntimeException("for " + x + ":" + y + ", ", ex);
			}
		};
	}

	public static IntPredicate_ predicate(IntPredicate_ predicate) {
		return c -> {
			try {
				return predicate.test(c);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", ", ex);
			}
		};
	}

}
