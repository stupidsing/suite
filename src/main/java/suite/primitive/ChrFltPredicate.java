package suite.primitive;

public interface ChrFltPredicate {

	public boolean test(char c, float f);

	public default ChrFltPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
