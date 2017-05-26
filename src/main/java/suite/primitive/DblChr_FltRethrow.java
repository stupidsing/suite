package suite.primitive;

public class DblChr_FltRethrow {

	public static DblChr_Flt fun2(DblChr_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
