package suite.primitive;

import suite.util.Fail;

public interface ChrInt_Lng {

	public long apply(char c, int f);

	public default ChrInt_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
