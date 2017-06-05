package suite.primitive;

public interface LngChrPredicate {

	public boolean test(long c, char f);

	public default LngChrPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
