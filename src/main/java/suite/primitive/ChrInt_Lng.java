package suite.primitive;

import static primal.statics.Fail.fail;

public interface ChrInt_Lng {

	public long apply(char c, int f);

	public default ChrInt_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
