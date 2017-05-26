package suite.primitive;

public class ChrSht_FltRethrow {

	public static ChrSht_Flt fun2(ChrSht_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
