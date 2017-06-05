package suite.primitive;

public interface IntLng_Flt {

	public float apply(int c, long f);

	public default IntLng_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
