package suite.primitive;

public interface FltLng_Int {

	public int apply(float c, long f);

	public default FltLng_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
