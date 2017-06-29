package suite.primitive;

public interface DblDbl_Dbl {

	public double apply(double c, double f);

	public default DblDbl_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
