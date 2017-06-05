package suite.primitive;

public class LngLng_DblRethrow {

	public static LngLng_Dbl fun2(LngLng_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
