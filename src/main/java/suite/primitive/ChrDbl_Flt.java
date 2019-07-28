package suite.primitive;

import static suite.util.Fail.fail;

public interface ChrDbl_Flt {

	public float apply(char c, double f);

	public default ChrDbl_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
