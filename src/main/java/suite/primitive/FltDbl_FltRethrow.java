package suite.primitive;

public class FltDbl_FltRethrow {

	public static FltDbl_Flt fun2(FltDbl_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
