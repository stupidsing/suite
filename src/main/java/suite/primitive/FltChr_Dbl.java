package suite.primitive;

import static primal.statics.Fail.fail;

public interface FltChr_Dbl {

	public double apply(float c, char f);

	public default FltChr_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
