package suite.primitive;

import static primal.statics.Fail.fail;

public interface DblLng_Dbl {

	public double apply(double c, long f);

	public default DblLng_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
