package suite.primitive;

import suite.primitive.DblFun.DblObj_Obj;
import suite.primitive.DblPredicate.DblObjPredicate;
import suite.primitive.DblPredicate.DblPredicate_;

public class DblRethrow {

	public static <T> DblObjPredicate<T> dblObjPredicate(DblObjPredicate<T> predicate) {
		return (c, t) -> {
			try {
				return predicate.test(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

	public static <X, Y> DblObj_Obj<X, Y> fun2(DblObj_Obj<X, Y> predicate) {
		return (x, y) -> {
			try {
				return predicate.apply(x, y);
			} catch (Exception ex) {
				throw new RuntimeException("for " + x + ":" + y + ", ", ex);
			}
		};
	}

	public static DblPredicate_ predicate(DblPredicate_ predicate) {
		return c -> {
			try {
				return predicate.test(c);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", ", ex);
			}
		};
	}

}
