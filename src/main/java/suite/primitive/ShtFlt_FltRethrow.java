package suite.primitive;

public class ShtFlt_FltRethrow {

	public static ShtFlt_Flt fun2(ShtFlt_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
