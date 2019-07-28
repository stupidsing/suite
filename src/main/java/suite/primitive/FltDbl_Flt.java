package suite.primitive;

import static suite.util.Fail.fail;

public interface FltDbl_Flt {

	public float apply(float c, double f);

	public default FltDbl_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
