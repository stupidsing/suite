package suite.primitive;

import static suite.util.Friends.fail;

public interface LngFlt_Int {

	public int apply(long c, float f);

	public default LngFlt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
