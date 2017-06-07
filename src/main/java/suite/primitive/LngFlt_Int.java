package suite.primitive;

public interface LngFlt_Int {

	public int apply(long c, float f);

	public default LngFlt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
