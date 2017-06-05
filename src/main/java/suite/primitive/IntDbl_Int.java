package suite.primitive;

public interface IntDbl_Int {

	public int apply(int c, double f);

	public default IntDbl_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
