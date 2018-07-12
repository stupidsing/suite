package suite.primitive;

import static suite.util.Friends.fail;

public interface DblDbl_Dbl {

	public double apply(double c, double f);

	public default DblDbl_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
