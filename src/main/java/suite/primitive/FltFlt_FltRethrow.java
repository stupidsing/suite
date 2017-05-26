package suite.primitive;

public class FltFlt_FltRethrow {

	public static FltFlt_Flt fun2(FltFlt_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
