package suite.primitive;

public class ShtChr_FltRethrow {

	public static ShtChr_Flt fun2(ShtChr_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
