package suite.primitive;

public interface FltInt_Flt {

	public float apply(float c, int f);

	public default FltInt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
