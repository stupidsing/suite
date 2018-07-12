package suite.primitive;

import static suite.util.Friends.fail;

public interface IntLng_Int {

	public int apply(int c, long f);

	public default IntLng_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
