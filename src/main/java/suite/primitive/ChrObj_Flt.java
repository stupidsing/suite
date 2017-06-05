package suite.primitive;

public interface ChrObj_Flt<T> {

	public float apply(char c, T t);

	public default ChrObj_Flt<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
