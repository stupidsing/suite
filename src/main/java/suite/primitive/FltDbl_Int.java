package suite.primitive;

import static suite.util.Fail.fail;

public interface FltDbl_Int {

	public int apply(float c, double f);

	public default FltDbl_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
