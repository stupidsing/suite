package suite.primitive;

public interface LngLng_Int {

	public int apply(long c, long f);

	public default LngLng_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
