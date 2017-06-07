package suite.primitive;

public interface LngDbl_Dbl {

	public double apply(long c, double f);

	public default LngDbl_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
