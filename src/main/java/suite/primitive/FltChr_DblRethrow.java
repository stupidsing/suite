package suite.primitive;

public class FltChr_DblRethrow {

	public static FltChr_Dbl fun2(FltChr_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
