package suite.primitive;

public interface FltDbl_Dbl {

	public double apply(float c, double f);

	public default FltDbl_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
