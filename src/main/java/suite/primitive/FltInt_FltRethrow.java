package suite.primitive;

public class FltInt_FltRethrow {

	public static FltInt_Flt fun2(FltInt_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
