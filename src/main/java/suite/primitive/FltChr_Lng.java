package suite.primitive;

import suite.util.Fail;

public interface FltChr_Lng {

	public long apply(float c, char f);

	public default FltChr_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
