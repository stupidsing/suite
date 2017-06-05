package suite.primitive;

public class LngLng_ChrRethrow {

	public static LngLng_Chr fun2(LngLng_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
