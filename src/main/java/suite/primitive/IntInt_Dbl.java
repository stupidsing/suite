package suite.primitive;

import suite.util.Fail;

public interface IntInt_Dbl {

	public double apply(int c, int f);

	public default IntInt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
