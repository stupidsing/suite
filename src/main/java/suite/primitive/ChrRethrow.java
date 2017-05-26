package suite.primitive;

import suite.primitive.ChrFun.ChrObj_Obj;
import suite.primitive.ChrPredicate.ChrObjPredicate;
import suite.primitive.ChrPredicate.ChrPredicate_;

public class ChrRethrow {

	public static <T> ChrObjPredicate<T> chrObjPredicate(ChrObjPredicate<T> predicate) {
		return (c, t) -> {
			try {
				return predicate.test(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

	public static <X, Y> ChrObj_Obj<X, Y> fun2(ChrObj_Obj<X, Y> predicate) {
		return (x, y) -> {
			try {
				return predicate.apply(x, y);
			} catch (Exception ex) {
				throw new RuntimeException("for " + x + ":" + y + ", ", ex);
			}
		};
	}

	public static ChrPredicate_ predicate(ChrPredicate_ predicate) {
		return c -> {
			try {
				return predicate.test(c);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", ", ex);
			}
		};
	}

}
