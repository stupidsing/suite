package suite.primitive;

public interface ChrDblPredicate {

	public boolean test(char c, double f);

	public default ChrDblPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
