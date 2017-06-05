package suite.primitive;

public class FltLng_LngRethrow {

	public static FltLng_Lng fun2(FltLng_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
