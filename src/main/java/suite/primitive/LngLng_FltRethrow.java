package suite.primitive;

public class LngLng_FltRethrow {

	public static LngLng_Flt fun2(LngLng_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
