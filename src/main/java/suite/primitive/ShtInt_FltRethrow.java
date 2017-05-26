package suite.primitive;

public class ShtInt_FltRethrow {

	public static ShtInt_Flt fun2(ShtInt_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
