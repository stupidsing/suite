package suite.primitive;

import static primal.statics.Fail.fail;

public interface DblDbl_Int {

	public int apply(double c, double f);

	public default DblDbl_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
