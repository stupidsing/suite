package suite.primitive;

public class IntInt_FltRethrow {

	public static IntInt_Flt fun2(IntInt_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
