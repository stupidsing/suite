package suite.primitive;

public interface DblIntPredicate {

	public boolean test(double c, int f);

	public default DblIntPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
