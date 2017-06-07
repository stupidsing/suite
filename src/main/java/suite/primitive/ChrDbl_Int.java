package suite.primitive;

public interface ChrDbl_Int {

	public int apply(char c, double f);

	public default ChrDbl_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
