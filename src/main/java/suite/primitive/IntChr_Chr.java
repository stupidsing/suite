package suite.primitive;

import static primal.statics.Fail.fail;

public interface IntChr_Chr {

	public char apply(int c, char f);

	public default IntChr_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
