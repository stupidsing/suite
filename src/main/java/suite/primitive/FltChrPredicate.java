package suite.primitive;

public interface FltChrPredicate {

	public boolean test(float c, char f);

	public default FltChrPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
