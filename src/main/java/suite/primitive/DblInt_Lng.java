package suite.primitive;

import static suite.util.Fail.fail;

public interface DblInt_Lng {

	public long apply(double c, int f);

	public default DblInt_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
