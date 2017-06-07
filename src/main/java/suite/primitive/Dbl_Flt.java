package suite.primitive;

@FunctionalInterface
public interface Dbl_Flt {

	public float apply(double c);

	public default Dbl_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
