package suite.primitive;

public interface ChrIntPredicate {

	public boolean test(char c, int f);

	public default ChrIntPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
