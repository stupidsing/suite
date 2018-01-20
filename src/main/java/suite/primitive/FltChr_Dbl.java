package suite.primitive;

import suite.util.Fail;

public interface FltChr_Dbl {

	public double apply(float c, char f);

	public default FltChr_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
