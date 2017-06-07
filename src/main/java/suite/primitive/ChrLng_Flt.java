package suite.primitive;

public interface ChrLng_Flt {

	public float apply(char c, long f);

	public default ChrLng_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
