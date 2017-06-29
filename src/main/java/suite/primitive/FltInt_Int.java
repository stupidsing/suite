package suite.primitive;

public interface FltInt_Int {

	public int apply(float c, int f);

	public default FltInt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
