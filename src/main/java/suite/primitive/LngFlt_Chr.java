package suite.primitive;

import static suite.util.Friends.fail;

public interface LngFlt_Chr {

	public char apply(long c, float f);

	public default LngFlt_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
