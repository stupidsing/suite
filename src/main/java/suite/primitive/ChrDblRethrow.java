package suite.primitive;

public class ChrDblRethrow {

	public static ChrDblPredicate chrDblPredicate(ChrDblPredicate predicate) {
		return (c, f) -> {
			try {
				return predicate.test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

	public static <T> ChrDbl_Obj<T> fun2(ChrDbl_Obj<T> fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

	public static <T> ChrObj_Dbl<T> fun2(ChrObj_Dbl<T> fun) {
		return (c, t) -> {
			try {
				return fun.apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
