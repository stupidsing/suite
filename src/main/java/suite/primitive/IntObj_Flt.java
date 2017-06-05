package suite.primitive;

public interface IntObj_Flt<T> {

	public float apply(int c, T t);

	public default IntObj_Flt<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
