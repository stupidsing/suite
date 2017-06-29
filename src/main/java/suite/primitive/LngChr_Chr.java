package suite.primitive;

public interface LngChr_Chr {

	public char apply(long c, char f);

	public default LngChr_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
