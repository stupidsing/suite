package suite.primitive;

public interface FltLng_Dbl {

	public double apply(float c, long f);

	public default FltLng_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
