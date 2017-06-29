package suite.primitive;

public interface FltLng_Flt {

	public float apply(float c, long f);

	public default FltLng_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
