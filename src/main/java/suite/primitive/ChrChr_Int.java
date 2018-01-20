package suite.primitive;

import suite.util.Fail;

public interface ChrChr_Int {

	public int apply(char c, char f);

	public default ChrChr_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
