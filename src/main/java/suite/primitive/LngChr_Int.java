package suite.primitive;

import static primal.statics.Fail.fail;

public interface LngChr_Int {

	public int apply(long c, char f);

	public default LngChr_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
