package suite.primitive;

public interface IntFltPredicate {

	public boolean test(int c, float f);

	public default IntFltPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
