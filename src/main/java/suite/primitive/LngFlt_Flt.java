package suite.primitive;

public interface LngFlt_Flt {

	public float apply(long c, float f);

	public default LngFlt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
