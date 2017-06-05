package suite.primitive;

public class DblLng_FltRethrow {

	public static DblLng_Flt fun2(DblLng_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
