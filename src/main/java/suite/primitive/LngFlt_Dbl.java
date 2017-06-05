package suite.primitive;

public interface LngFlt_Dbl {

	public double apply(long c, float f);

	public default LngFlt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
