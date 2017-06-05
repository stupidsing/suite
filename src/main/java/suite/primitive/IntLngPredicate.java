package suite.primitive;

public interface IntLngPredicate {

	public boolean test(int c, long f);

	public default IntLngPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
