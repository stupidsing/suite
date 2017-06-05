package suite.primitive;

public interface DblFltPredicate {

	public boolean test(double c, float f);

	public default DblFltPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
