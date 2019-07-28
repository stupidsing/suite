package suite.primitive;

import static suite.util.Fail.fail;

public interface LngChr_Lng {

	public long apply(long c, char f);

	public default LngChr_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
