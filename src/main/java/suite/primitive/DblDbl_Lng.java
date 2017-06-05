package suite.primitive;

public interface DblDbl_Lng {

	public long apply(double c, double f);

	public default DblDbl_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
