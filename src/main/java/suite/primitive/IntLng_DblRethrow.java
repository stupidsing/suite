package suite.primitive;

public class IntLng_DblRethrow {

	public static IntLng_Dbl fun2(IntLng_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
