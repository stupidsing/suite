package suite.primitive;

public interface LngDbl_Chr {

	public char apply(long c, double f);

	public default LngDbl_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
