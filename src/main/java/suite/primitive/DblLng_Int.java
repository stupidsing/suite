package suite.primitive;

import suite.util.Fail;

public interface DblLng_Int {

	public int apply(double c, long f);

	public default DblLng_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
