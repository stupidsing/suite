package suite.primitive;

import suite.util.Fail;

public interface IntChr_Dbl {

	public double apply(int c, char f);

	public default IntChr_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
