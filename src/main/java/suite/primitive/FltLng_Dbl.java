package suite.primitive;

import static suite.util.Fail.fail;

public interface FltLng_Dbl {

	public double apply(float c, long f);

	public default FltLng_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
