package suite.primitive;

public interface LngLng_Lng {

	public long apply(long c, long f);

	public default LngLng_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
