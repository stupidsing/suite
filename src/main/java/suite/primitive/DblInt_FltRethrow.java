package suite.primitive;

public class DblInt_FltRethrow {

	public static DblInt_Flt fun2(DblInt_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
