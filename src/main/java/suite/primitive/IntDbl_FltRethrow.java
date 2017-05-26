package suite.primitive;

public class IntDbl_FltRethrow {

	public static IntDbl_Flt fun2(IntDbl_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
