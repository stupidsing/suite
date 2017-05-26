package suite.primitive;

public class DblFlt_FltRethrow {

	public static DblFlt_Flt fun2(DblFlt_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
