package suite.primitive;

public class ChrChr_DblRethrow {

	public static ChrChr_Dbl fun2(ChrChr_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
