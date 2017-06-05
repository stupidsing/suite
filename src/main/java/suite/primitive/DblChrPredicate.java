package suite.primitive;

public interface DblChrPredicate {

	public boolean test(double c, char f);

	public default DblChrPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
