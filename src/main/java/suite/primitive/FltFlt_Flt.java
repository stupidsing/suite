package suite.primitive;

import static primal.statics.Fail.fail;

public interface FltFlt_Flt {

	public float apply(float c, float f);

	public default FltFlt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
