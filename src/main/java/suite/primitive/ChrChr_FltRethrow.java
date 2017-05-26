package suite.primitive;

public class ChrChr_FltRethrow {

	public static ChrChr_Flt fun2(ChrChr_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
