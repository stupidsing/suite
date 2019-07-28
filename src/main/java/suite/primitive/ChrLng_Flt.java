package suite.primitive;

import static suite.util.Fail.fail;

public interface ChrLng_Flt {

	public float apply(char c, long f);

	public default ChrLng_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
