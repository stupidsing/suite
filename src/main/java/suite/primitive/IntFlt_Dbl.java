package suite.primitive;

public interface IntFlt_Dbl {

	public double apply(int c, float f);

	public default IntFlt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
