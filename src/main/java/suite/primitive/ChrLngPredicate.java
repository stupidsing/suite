package suite.primitive;

public interface ChrLngPredicate {

	public boolean test(char c, long f);

	public default ChrLngPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
