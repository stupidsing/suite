package suite.primitive;

import static primal.statics.Fail.fail;

public interface DblFlt_Int {

	public int apply(double c, float f);

	public default DblFlt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
