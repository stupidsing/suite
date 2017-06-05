package suite.primitive;

public interface FltDblPredicate {

	public boolean test(float c, double f);

	public default FltDblPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
