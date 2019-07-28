package suite.primitive;

import static suite.util.Fail.fail;

public interface IntFlt_Dbl {

	public double apply(int c, float f);

	public default IntFlt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
