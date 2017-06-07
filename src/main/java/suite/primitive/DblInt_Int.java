package suite.primitive;

public interface DblInt_Int {

	public int apply(double c, int f);

	public default DblInt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
