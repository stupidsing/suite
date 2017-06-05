package suite.primitive;

public class LngChr_FltRethrow {

	public static LngChr_Flt fun2(LngChr_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
