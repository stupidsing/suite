package suite.primitive;

import static suite.util.Fail.fail;

public interface IntInt_Bool {

	public boolean apply(int c, int f);

	public default IntInt_Bool rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
