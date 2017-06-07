package suite.primitive;

@FunctionalInterface
public interface Int_Chr {

	public char apply(int c);

	public default Int_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
