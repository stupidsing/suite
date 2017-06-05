package suite.primitive;

public class ChrLng_DblRethrow {

	public static ChrLng_Dbl fun2(ChrLng_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
