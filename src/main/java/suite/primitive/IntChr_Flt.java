package suite.primitive;

public interface IntChr_Flt {

	public float apply(int c, char f);

	public default IntChr_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
