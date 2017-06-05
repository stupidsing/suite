package suite.primitive;

public interface IntDbl_Flt {

	public float apply(int c, double f);

	public default IntDbl_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
