package suite.primitive;

@FunctionalInterface
public interface Chr_Lng {

	public long apply(char c);

	public default Chr_Lng rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
