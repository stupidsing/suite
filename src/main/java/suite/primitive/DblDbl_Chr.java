package suite.primitive;

import static suite.util.Fail.fail;

public interface DblDbl_Chr {

	public char apply(double c, double f);

	public default DblDbl_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
