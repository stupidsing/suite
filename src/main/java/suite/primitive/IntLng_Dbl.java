package suite.primitive;

import static suite.util.Fail.fail;

public interface IntLng_Dbl {

	public double apply(int c, long f);

	public default IntLng_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
