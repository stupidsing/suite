package suite.primitive;

public interface DblChr_Int {

	public int apply(double c, char f);

	public default DblChr_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
