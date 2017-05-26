package suite.primitive;

public class FltFlt_DblRethrow {

	public static FltFlt_Dbl fun2(FltFlt_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
