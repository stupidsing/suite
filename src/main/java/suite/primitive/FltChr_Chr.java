package suite.primitive;

import static suite.util.Fail.fail;

public interface FltChr_Chr {

	public char apply(float c, char f);

	public default FltChr_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
