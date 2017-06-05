package suite.primitive;

public interface LngInt_Flt {

	public float apply(long c, int f);

	public default LngInt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
