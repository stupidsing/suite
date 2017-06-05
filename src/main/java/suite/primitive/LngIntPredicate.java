package suite.primitive;

public interface LngIntPredicate {

	public boolean test(long c, int f);

	public default LngIntPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
