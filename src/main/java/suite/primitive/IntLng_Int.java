package suite.primitive;

public interface IntLng_Int {

	public int apply(int c, long f);

	public default IntLng_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
