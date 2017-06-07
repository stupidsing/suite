package suite.primitive;

@FunctionalInterface
public interface Chr_Chr {

	public char apply(char c);

	public default Chr_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
