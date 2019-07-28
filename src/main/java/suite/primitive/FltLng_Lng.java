package suite.primitive;

import static suite.util.Fail.fail;

public interface FltLng_Lng {

	public long apply(float c, long f);

	public default FltLng_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
