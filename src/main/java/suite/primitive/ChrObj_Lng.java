package suite.primitive;

public interface ChrObj_Lng<T> {

	public long apply(char c, T t);

	public default ChrObj_Lng<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
