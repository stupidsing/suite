package suite.primitive;

public interface ChrDbl_Dbl {

	public double apply(char c, double f);

	public default ChrDbl_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
