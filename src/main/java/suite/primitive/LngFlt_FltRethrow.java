package suite.primitive;

public class LngFlt_FltRethrow {

	public static LngFlt_Flt fun2(LngFlt_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
