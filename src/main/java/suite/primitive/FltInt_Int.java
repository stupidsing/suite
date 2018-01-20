package suite.primitive;

import suite.util.Fail;

public interface FltInt_Int {

	public int apply(float c, int f);

	public default FltInt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
