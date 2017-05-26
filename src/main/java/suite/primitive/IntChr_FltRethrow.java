package suite.primitive;

public class IntChr_FltRethrow {

	public static IntChr_Flt fun2(IntChr_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
