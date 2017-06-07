package suite.primitive;

public interface FltFlt_Flt {

	public float apply(float c, float f);

	public default FltFlt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
