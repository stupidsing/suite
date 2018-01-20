package suite.primitive;

import suite.util.Fail;

public interface LngChr_Flt {

	public float apply(long c, char f);

	public default LngChr_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
