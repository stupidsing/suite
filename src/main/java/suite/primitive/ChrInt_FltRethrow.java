package suite.primitive;

public class ChrInt_FltRethrow {

	public static ChrInt_Flt fun2(ChrInt_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
