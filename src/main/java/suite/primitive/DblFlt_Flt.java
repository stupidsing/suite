package suite.primitive;

import static primal.statics.Fail.fail;

public interface DblFlt_Flt {

	public float apply(double c, float f);

	public default DblFlt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
