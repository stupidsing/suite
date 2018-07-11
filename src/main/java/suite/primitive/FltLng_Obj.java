package suite.primitive; import static suite.util.Friends.fail;

public interface FltLng_Obj<T> {

	public T apply(float c, long f);

	public default FltLng_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
