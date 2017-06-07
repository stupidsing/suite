package suite.primitive;

@FunctionalInterface
public interface Chr_Dbl {

	public double apply(char c);

	public default Chr_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
