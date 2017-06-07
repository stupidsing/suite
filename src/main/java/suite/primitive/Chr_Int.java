package suite.primitive;

@FunctionalInterface
public interface Chr_Int {

	public int apply(char c);

	public default Chr_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
