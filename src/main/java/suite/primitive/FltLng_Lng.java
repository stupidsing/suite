package suite.primitive;

public interface FltLng_Lng {

	public long apply(float c, long f);

	public default FltLng_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
