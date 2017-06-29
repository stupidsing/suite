package suite.primitive;

public interface IntDbl_Chr {

	public char apply(int c, double f);

	public default IntDbl_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
