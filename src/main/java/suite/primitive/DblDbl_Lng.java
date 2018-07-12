package suite.primitive;

import static suite.util.Friends.fail;

public interface DblDbl_Lng {

	public long apply(double c, double f);

	public default DblDbl_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
