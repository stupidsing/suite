package suite.primitive;

import static suite.util.Fail.fail;

public interface DblFlt_Lng {

	public long apply(double c, float f);

	public default DblFlt_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
