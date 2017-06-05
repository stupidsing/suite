package suite.primitive;

public interface DblLngPredicate {

	public boolean test(double c, long f);

	public default DblLngPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
