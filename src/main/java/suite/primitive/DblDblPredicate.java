package suite.primitive;

public interface DblDblPredicate {

	public boolean test(double c, double f);

	public default DblDblPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
