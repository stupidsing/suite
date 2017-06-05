package suite.primitive;

public interface LngDbl_Lng {

	public long apply(long c, double f);

	public default LngDbl_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
