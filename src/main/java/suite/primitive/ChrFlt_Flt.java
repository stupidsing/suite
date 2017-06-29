package suite.primitive;

public interface ChrFlt_Flt {

	public float apply(char c, float f);

	public default ChrFlt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
