package suite.primitive;

public class ChrLngRethrow {

	public static ChrLngPredicate chrLngPredicate(ChrLngPredicate predicate) {
		return (c, f) -> {
			try {
				return predicate.test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

	public static <T> ChrLng_Obj<T> fun2(ChrLng_Obj<T> fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

	public static <T> ChrObj_Lng<T> fun2(ChrObj_Lng<T> fun) {
		return (c, t) -> {
			try {
				return fun.apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
