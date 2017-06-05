package suite.primitive;

public class LngInt_FltRethrow {

	public static LngInt_Flt fun2(LngInt_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
