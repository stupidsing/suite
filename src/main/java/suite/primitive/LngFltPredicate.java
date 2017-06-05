package suite.primitive;

public interface LngFltPredicate {

	public boolean test(long c, float f);

	public default LngFltPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
