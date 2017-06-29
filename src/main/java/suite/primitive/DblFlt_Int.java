package suite.primitive;

public interface DblFlt_Int {

	public int apply(double c, float f);

	public default DblFlt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
