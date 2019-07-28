package suite.primitive;

import static suite.util.Fail.fail;

public interface IntInt_Int {

	public int apply(int c, int f);

	public default IntInt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
