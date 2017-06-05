package suite.primitive;

public interface IntFlt_Int {

	public int apply(int c, float f);

	public default IntFlt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
