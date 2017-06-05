package suite.primitive;

public interface IntObj_Lng<T> {

	public long apply(int c, T t);

	public default IntObj_Lng<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
