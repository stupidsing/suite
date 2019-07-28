package suite.primitive;

import static suite.util.Fail.fail;

public interface LngChr_Chr {

	public char apply(long c, char f);

	public default LngChr_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
