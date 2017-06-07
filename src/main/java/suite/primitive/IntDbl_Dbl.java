package suite.primitive;

public interface IntDbl_Dbl {

	public double apply(int c, double f);

	public default IntDbl_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
