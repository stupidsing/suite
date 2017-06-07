package suite.primitive;

public interface ChrChr_Int {

	public int apply(char c, char f);

	public default ChrChr_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
