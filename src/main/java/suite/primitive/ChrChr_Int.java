package suite.primitive;

import static primal.statics.Fail.fail;

public interface ChrChr_Int {

	public int apply(char c, char f);

	public default ChrChr_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
