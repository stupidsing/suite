package suite.primitive;

public interface ChrFlt_Int {

	public int apply(char c, float f);

	public default ChrFlt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
