package suite.primitive;

public interface ChrChrPredicate {

	public boolean test(char c, char f);

	public default ChrChrPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
