package suite.primitive;

public class LngDbl_FltRethrow {

	public static LngDbl_Flt fun2(LngDbl_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
