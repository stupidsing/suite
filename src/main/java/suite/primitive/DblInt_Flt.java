package suite.primitive;

import static suite.util.Fail.fail;

public interface DblInt_Flt {

	public float apply(double c, int f);

	public default DblInt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
