package suite.primitive;

import static suite.util.Fail.fail;

public interface DblFlt_Dbl {

	public double apply(double c, float f);

	public default DblFlt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
