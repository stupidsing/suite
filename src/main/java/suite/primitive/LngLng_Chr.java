package suite.primitive;

public interface LngLng_Chr {

	public char apply(long c, long f);

	public default LngLng_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
