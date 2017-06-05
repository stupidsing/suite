package suite.primitive;

public interface FltLngPredicate {

	public boolean test(float c, long f);

	public default FltLngPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
