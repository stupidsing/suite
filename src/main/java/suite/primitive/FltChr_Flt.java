package suite.primitive;

public interface FltChr_Flt {

	public float apply(float c, char f);

	public default FltChr_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
