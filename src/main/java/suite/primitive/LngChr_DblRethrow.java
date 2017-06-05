package suite.primitive;

public class LngChr_DblRethrow {

	public static LngChr_Dbl fun2(LngChr_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
