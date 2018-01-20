package suite.primitive;

import suite.util.Fail;

public interface DblFlt_Dbl {

	public double apply(double c, float f);

	public default DblFlt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
