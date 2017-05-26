package suite.primitive;

public class ShtFltRethrow {

	public static ShtFltPredicate shtFltPredicate(ShtFltPredicate predicate) {
		return (c, f) -> {
			try {
				return predicate.test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

	public static <T> ShtFlt_Obj<T> fun2(ShtFlt_Obj<T> fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

	public static <T> ShtObj_Flt<T> fun2(ShtObj_Flt<T> fun) {
		return (c, t) -> {
			try {
				return fun.apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
