package suite.primitive;

public interface ChrDbl_Chr {

	public char apply(char c, double f);

	public default ChrDbl_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
