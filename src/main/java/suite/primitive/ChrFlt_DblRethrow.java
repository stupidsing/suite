package suite.primitive;

public class ChrFlt_DblRethrow {

	public static ChrFlt_Dbl fun2(ChrFlt_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
