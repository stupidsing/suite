package suite.primitive;

public interface DblLng_Flt {

	public float apply(double c, long f);

	public default DblLng_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
