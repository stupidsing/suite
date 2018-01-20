package suite.primitive;

import suite.util.Fail;

public interface LngChr_Dbl {

	public double apply(long c, char f);

	public default LngChr_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
