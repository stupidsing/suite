package suite.primitive;

@FunctionalInterface
public interface Flt_Chr {

	public char apply(float c);

	public default Flt_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
