package suite.primitive;

import suite.util.Fail;

public interface IntInt_Chr {

	public char apply(int c, int f);

	public default IntInt_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
