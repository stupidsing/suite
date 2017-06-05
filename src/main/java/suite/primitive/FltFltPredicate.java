package suite.primitive;

public interface FltFltPredicate {

	public boolean test(float c, float f);

	public default FltFltPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
