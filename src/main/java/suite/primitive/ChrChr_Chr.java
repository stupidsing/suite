package suite.primitive;

public interface ChrChr_Chr {

	public char apply(char c, char f);

	public default ChrChr_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
