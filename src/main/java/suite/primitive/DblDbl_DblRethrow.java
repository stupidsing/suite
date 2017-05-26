package suite.primitive;

public class DblDbl_DblRethrow {

	public static DblDbl_Dbl fun2(DblDbl_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
