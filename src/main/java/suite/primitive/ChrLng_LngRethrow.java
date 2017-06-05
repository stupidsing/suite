package suite.primitive;

public class ChrLng_LngRethrow {

	public static ChrLng_Lng fun2(ChrLng_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
