package suite.primitive;

public class DblLng_DblRethrow {

	public static DblLng_Dbl fun2(DblLng_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
