package suite.primitive;

import static primal.statics.Fail.fail;

public interface FltChr_Flt {

	public float apply(float c, char f);

	public default FltChr_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
