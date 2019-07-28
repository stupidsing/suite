package suite.primitive;

import static suite.util.Fail.fail;

public interface ChrInt_Dbl {

	public double apply(char c, int f);

	public default ChrInt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
