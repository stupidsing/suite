package suite.primitive;

import static suite.util.Friends.fail;

public interface IntLng_Flt {

	public float apply(int c, long f);

	public default IntLng_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
