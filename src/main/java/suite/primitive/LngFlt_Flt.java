package suite.primitive;

import static primal.statics.Fail.fail;

public interface LngFlt_Flt {

	public float apply(long c, float f);

	public default LngFlt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
