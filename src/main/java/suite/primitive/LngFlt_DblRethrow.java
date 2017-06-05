package suite.primitive;

public class LngFlt_DblRethrow {

	public static LngFlt_Dbl fun2(LngFlt_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
