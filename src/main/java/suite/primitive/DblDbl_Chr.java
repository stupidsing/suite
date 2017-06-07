package suite.primitive;

public interface DblDbl_Chr {

	public char apply(double c, double f);

	public default DblDbl_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
