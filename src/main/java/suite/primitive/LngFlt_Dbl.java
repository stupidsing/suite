package suite.primitive;

import static suite.util.Fail.fail;

public interface LngFlt_Dbl {

	public double apply(long c, float f);

	public default LngFlt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
