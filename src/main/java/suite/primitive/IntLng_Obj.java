package suite.primitive; import static suite.util.Friends.fail;

public interface IntLng_Obj<T> {

	public T apply(int c, long f);

	public default IntLng_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
