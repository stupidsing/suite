package suite.primitive;

import static primal.statics.Fail.fail;

public interface LngLng_Int {

	public int apply(long c, long f);

	public default LngLng_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
