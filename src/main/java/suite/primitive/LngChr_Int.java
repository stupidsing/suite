package suite.primitive;

public interface LngChr_Int {

	public int apply(long c, char f);

	public default LngChr_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
