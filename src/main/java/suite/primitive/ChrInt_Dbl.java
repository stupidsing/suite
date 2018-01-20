package suite.primitive;

import suite.util.Fail;

public interface ChrInt_Dbl {

	public double apply(char c, int f);

	public default ChrInt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
