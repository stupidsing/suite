package suite.primitive;

public class DblChr_DblRethrow {

	public static DblChr_Dbl fun2(DblChr_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
