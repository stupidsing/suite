package suite.primitive;

public interface LngInt_Int {

	public int apply(long c, int f);

	public default LngInt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
