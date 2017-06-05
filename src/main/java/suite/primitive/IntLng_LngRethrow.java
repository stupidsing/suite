package suite.primitive;

public class IntLng_LngRethrow {

	public static IntLng_Lng fun2(IntLng_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
