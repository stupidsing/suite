package suite.primitive;

import static suite.util.Friends.fail;

public interface ChrDbl_Chr {

	public char apply(char c, double f);

	public default ChrDbl_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
