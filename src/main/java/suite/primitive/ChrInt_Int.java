package suite.primitive;

public interface ChrInt_Int {

	public int apply(char c, int f);

	public default ChrInt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
