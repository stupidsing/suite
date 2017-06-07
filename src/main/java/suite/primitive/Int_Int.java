package suite.primitive;

@FunctionalInterface
public interface Int_Int {

	public int apply(int c);

	public default Int_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
