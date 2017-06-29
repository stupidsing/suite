package suite.primitive;

public interface LngDbl_Flt {

	public float apply(long c, double f);

	public default LngDbl_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
