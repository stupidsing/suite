package suite.primitive;

public class LngLng_IntRethrow {

	public static LngLng_Int fun2(LngLng_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
