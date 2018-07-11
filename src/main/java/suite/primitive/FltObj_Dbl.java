package suite.primitive; import static suite.util.Friends.fail;

public interface FltObj_Dbl<T> {

	public double apply(float c, T t);

	public default FltObj_Dbl<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return fail("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
