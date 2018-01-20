package suite.primitive;

import suite.util.Fail;

public interface LngChr_Lng {

	public long apply(long c, char f);

	public default LngChr_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
