package suite.primitive;

@FunctionalInterface
public interface Chr_Flt {

	public float apply(char c);

	public default Chr_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
