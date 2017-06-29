package suite.primitive;

public interface ChrChr_Dbl {

	public double apply(char c, char f);

	public default ChrChr_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
