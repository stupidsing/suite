package suite.primitive;

public interface IntDbl_Lng {

	public long apply(int c, double f);

	public default IntDbl_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
