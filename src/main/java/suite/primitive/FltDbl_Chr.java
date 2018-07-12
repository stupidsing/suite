package suite.primitive;

import static suite.util.Friends.fail;

public interface FltDbl_Chr {

	public char apply(float c, double f);

	public default FltDbl_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
