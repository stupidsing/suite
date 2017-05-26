package suite.primitive;

public class FltChr_FltRethrow {

	public static FltChr_Flt fun2(FltChr_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
