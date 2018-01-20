package suite.primitive;

import suite.util.Fail;

public interface ChrChr_Lng {

	public long apply(char c, char f);

	public default ChrChr_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
