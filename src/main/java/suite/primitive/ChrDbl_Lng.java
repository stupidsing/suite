package suite.primitive;

public interface ChrDbl_Lng {

	public long apply(char c, double f);

	public default ChrDbl_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
