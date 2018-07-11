package suite.primitive; import static suite.util.Friends.fail;

public interface ChrLng_Lng {

	public long apply(char c, long f);

	public default ChrLng_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
