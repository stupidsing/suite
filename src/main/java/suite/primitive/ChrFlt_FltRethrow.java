package suite.primitive;

public class ChrFlt_FltRethrow {

	public static ChrFlt_Flt fun2(ChrFlt_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
