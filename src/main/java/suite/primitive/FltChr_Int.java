package suite.primitive;

public interface FltChr_Int {

	public int apply(float c, char f);

	public default FltChr_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
