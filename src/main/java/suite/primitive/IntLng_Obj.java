package suite.primitive;

public interface IntLng_Obj<T> {

	public T apply(int c, long f);

	public default IntLng_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
