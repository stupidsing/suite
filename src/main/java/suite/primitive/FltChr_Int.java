package suite.primitive;

import static suite.util.Friends.fail;

public interface FltChr_Int {

	public int apply(float c, char f);

	public default FltChr_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
