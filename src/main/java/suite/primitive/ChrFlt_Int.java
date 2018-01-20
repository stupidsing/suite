package suite.primitive;

import suite.util.Fail;

public interface ChrFlt_Int {

	public int apply(char c, float f);

	public default ChrFlt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
