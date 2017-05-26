package suite.primitive;

public class FltSht_FltRethrow {

	public static FltSht_Flt fun2(FltSht_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
