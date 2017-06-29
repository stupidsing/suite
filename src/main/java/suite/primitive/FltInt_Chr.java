package suite.primitive;

public interface FltInt_Chr {

	public char apply(float c, int f);

	public default FltInt_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
