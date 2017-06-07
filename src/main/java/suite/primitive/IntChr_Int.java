package suite.primitive;

public interface IntChr_Int {

	public int apply(int c, char f);

	public default IntChr_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
