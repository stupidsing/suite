package suite.primitive;

import static suite.util.Friends.fail;

public interface LngDbl_Int {

	public int apply(long c, double f);

	public default LngDbl_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
