package suite.primitive;

public interface DblInt_Flt {

	public float apply(double c, int f);

	public default DblInt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
