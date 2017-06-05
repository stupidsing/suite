package suite.primitive;

public interface DblDbl_Int {

	public int apply(double c, double f);

	public default DblDbl_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
