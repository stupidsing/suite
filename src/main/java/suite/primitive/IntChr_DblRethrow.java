package suite.primitive;

public class IntChr_DblRethrow {

	public static IntChr_Dbl fun2(IntChr_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
