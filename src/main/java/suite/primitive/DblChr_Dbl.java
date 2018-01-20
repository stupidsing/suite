package suite.primitive;

import suite.util.Fail;

public interface DblChr_Dbl {

	public double apply(double c, char f);

	public default DblChr_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
