package suite.primitive;

import static suite.util.Fail.fail;

public interface DblChr_Int {

	public int apply(double c, char f);

	public default DblChr_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
