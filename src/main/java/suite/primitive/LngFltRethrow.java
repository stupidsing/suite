package suite.primitive;

public class LngFltRethrow {

	public static LngFltPredicate lngFltPredicate(LngFltPredicate predicate) {
		return (c, f) -> {
			try {
				return predicate.test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

	public static <T> LngFlt_Obj<T> fun2(LngFlt_Obj<T> fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

	public static <T> LngObj_Flt<T> fun2(LngObj_Flt<T> fun) {
		return (c, t) -> {
			try {
				return fun.apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
