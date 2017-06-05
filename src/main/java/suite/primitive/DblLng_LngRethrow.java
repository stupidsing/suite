package suite.primitive;

public class DblLng_LngRethrow {

	public static DblLng_Lng fun2(DblLng_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
