package suite.primitive;

public class IntFlt_FltRethrow {

	public static IntFlt_Flt fun2(IntFlt_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
