package suite.primitive;

public interface ChrLng_Int {

	public int apply(char c, long f);

	public default ChrLng_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
