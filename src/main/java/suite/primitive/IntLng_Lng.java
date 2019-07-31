package suite.primitive;

import static primal.statics.Fail.fail;

public interface IntLng_Lng {

	public long apply(int c, long f);

	public default IntLng_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
