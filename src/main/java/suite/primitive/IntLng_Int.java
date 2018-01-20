package suite.primitive;

import suite.util.Fail;

public interface IntLng_Int {

	public int apply(int c, long f);

	public default IntLng_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
