package suite.primitive;

public interface FltDbl_Lng {

	public long apply(float c, double f);

	public default FltDbl_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
