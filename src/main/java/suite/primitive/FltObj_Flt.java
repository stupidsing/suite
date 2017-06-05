package suite.primitive;

public interface FltObj_Flt<T> {

	public float apply(float c, T t);

	public default FltObj_Flt<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
