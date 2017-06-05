package suite.primitive;

public interface IntDblPredicate {

	public boolean test(int c, double f);

	public default IntDblPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
