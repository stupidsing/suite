package suite.primitive;

public interface ChrChr_Flt {

	public float apply(char c, char f);

	public default ChrChr_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
