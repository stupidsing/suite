package suite.primitive;

import static primal.statics.Fail.fail;

public interface ChrFlt_Lng {

	public long apply(char c, float f);

	public default ChrFlt_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
