package suite.primitive;

import static suite.util.Fail.fail;

public interface LngInt_Dbl {

	public double apply(long c, int f);

	public default LngInt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
