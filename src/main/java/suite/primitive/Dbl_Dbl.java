package suite.primitive;

@FunctionalInterface
public interface Dbl_Dbl {

	public double apply(double c);

	public default Dbl_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
