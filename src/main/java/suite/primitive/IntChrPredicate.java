package suite.primitive;

public interface IntChrPredicate {

	public boolean test(int c, char f);

	public default IntChrPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
