package suite.primitive;

public class DblFltRethrow {

	public static DblFltPredicate dblFltPredicate(DblFltPredicate predicate) {
		return (c, f) -> {
			try {
				return predicate.test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

	public static <T> DblFlt_Obj<T> fun2(DblFlt_Obj<T> fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

	public static <T> DblObj_Flt<T> fun2(DblObj_Flt<T> fun) {
		return (c, t) -> {
			try {
				return fun.apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
