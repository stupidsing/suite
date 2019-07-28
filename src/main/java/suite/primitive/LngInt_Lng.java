package suite.primitive;

import static suite.util.Fail.fail;

public interface LngInt_Lng {

	public long apply(long c, int f);

	public default LngInt_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
