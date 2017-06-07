package suite.primitive;

public interface FltDbl_Chr {

	public char apply(float c, double f);

	public default FltDbl_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
