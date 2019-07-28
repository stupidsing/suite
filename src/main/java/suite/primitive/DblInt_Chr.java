package suite.primitive;

import static suite.util.Fail.fail;

public interface DblInt_Chr {

	public char apply(double c, int f);

	public default DblInt_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
