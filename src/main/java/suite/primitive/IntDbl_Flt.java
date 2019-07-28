package suite.primitive;

import static suite.util.Fail.fail;

public interface IntDbl_Flt {

	public float apply(int c, double f);

	public default IntDbl_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
