package suite.primitive;

public class LngDbl_DblRethrow {

	public static LngDbl_Dbl fun2(LngDbl_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
