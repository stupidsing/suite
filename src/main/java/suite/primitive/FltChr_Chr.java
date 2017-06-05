package suite.primitive;

public interface FltChr_Chr {

	public char apply(float c, char f);

	public default FltChr_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
