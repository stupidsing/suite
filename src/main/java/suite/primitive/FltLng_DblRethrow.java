package suite.primitive;

public class FltLng_DblRethrow {

	public static FltLng_Dbl fun2(FltLng_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
