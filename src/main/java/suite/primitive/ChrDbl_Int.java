package suite.primitive;

import suite.util.Fail;

public interface ChrDbl_Int {

	public int apply(char c, double f);

	public default ChrDbl_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
