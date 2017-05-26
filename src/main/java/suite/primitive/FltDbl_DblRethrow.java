package suite.primitive;

public class FltDbl_DblRethrow {

	public static FltDbl_Dbl fun2(FltDbl_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
