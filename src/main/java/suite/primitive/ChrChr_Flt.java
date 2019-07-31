package suite.primitive;

import static primal.statics.Fail.fail;

public interface ChrChr_Flt {

	public float apply(char c, char f);

	public default ChrChr_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
