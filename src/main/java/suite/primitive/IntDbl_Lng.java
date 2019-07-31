package suite.primitive;

import static primal.statics.Fail.fail;

public interface IntDbl_Lng {

	public long apply(int c, double f);

	public default IntDbl_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
