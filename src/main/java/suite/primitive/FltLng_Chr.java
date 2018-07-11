package suite.primitive; import static suite.util.Friends.fail;

public interface FltLng_Chr {

	public char apply(float c, long f);

	public default FltLng_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
