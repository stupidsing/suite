package suite.primitive;

public class ChrInt_DblRethrow {

	public static ChrInt_Dbl fun2(ChrInt_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
