package suite.primitive;

public class IntLng_FltRethrow {

	public static IntLng_Flt fun2(IntLng_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
