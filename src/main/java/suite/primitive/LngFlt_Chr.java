package suite.primitive;

public interface LngFlt_Chr {

	public char apply(long c, float f);

	public default LngFlt_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
