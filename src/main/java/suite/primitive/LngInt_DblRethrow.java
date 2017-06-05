package suite.primitive;

public class LngInt_DblRethrow {

	public static LngInt_Dbl fun2(LngInt_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
