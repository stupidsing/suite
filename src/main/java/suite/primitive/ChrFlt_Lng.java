package suite.primitive;

import suite.util.Fail;

public interface ChrFlt_Lng {

	public long apply(char c, float f);

	public default ChrFlt_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
