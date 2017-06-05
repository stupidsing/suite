package suite.primitive;

public interface LngLngPredicate {

	public boolean test(long c, long f);

	public default LngLngPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
