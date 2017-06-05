package suite.primitive;

public interface LngDbl_Obj<T> {

	public T apply(long c, double f);

	public default LngDbl_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
