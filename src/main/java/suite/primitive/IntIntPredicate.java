package suite.primitive;

public interface IntIntPredicate {

	public boolean test(int c, int f);

	public default IntIntPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
