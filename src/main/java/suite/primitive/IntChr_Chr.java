package suite.primitive;

import suite.util.Fail;

public interface IntChr_Chr {

	public char apply(int c, char f);

	public default IntChr_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
