package suite.primitive;

public class ChrLng_FltRethrow {

	public static ChrLng_Flt fun2(ChrLng_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
