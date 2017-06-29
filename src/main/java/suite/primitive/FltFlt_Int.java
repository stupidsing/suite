package suite.primitive;

public interface FltFlt_Int {

	public int apply(float c, float f);

	public default FltFlt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
