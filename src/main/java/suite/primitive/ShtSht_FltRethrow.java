package suite.primitive;

public class ShtSht_FltRethrow {

	public static ShtSht_Flt fun2(ShtSht_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
