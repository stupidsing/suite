package suite.primitive;

import static primal.statics.Fail.fail;

public interface FltInt_Chr {

	public char apply(float c, int f);

	public default FltInt_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
