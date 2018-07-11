package suite.primitive; import static suite.util.Friends.fail;

public interface LngLng_Flt {

	public float apply(long c, long f);

	public default LngLng_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
