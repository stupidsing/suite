package suite.primitive;

public class LngLng_LngRethrow {

	public static LngLng_Lng fun2(LngLng_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
