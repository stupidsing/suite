package suite.primitive; import static suite.util.Friends.fail;

public interface FltLng_Int {

	public int apply(float c, long f);

	public default FltLng_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
