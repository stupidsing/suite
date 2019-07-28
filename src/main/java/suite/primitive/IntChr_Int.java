package suite.primitive;

import static suite.util.Fail.fail;

public interface IntChr_Int {

	public int apply(int c, char f);

	public default IntChr_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
