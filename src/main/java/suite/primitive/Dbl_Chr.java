package suite.primitive;

@FunctionalInterface
public interface Dbl_Chr {

	public char apply(double c);

	public default Dbl_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
