package suite.primitive;

import static primal.statics.Fail.fail;

public interface LngLng_Dbl {

	public double apply(long c, long f);

	public default LngLng_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
