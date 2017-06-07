package suite.primitive;

@FunctionalInterface
public interface Flt_Flt {

	public float apply(float c);

	public default Flt_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
