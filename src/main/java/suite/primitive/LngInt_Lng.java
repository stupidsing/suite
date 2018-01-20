package suite.primitive;

import suite.util.Fail;

public interface LngInt_Lng {

	public long apply(long c, int f);

	public default LngInt_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
