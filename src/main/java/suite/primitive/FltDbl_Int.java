package suite.primitive;

public interface FltDbl_Int {

	public int apply(float c, double f);

	public default FltDbl_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
