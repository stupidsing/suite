package suite.primitive;

public class FltLng_FltRethrow {

	public static FltLng_Flt fun2(FltLng_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
