package suite.primitive;

import static suite.util.Fail.fail;

public interface LngLng_Lng {

	public long apply(long c, long f);

	public default LngLng_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
