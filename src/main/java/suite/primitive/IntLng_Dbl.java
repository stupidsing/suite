package suite.primitive;

import suite.util.Fail;

public interface IntLng_Dbl {

	public double apply(int c, long f);

	public default IntLng_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
