package suite.primitive;

public interface FltIntPredicate {

	public boolean test(float c, int f);

	public default FltIntPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
