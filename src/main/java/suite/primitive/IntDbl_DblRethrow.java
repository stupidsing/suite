package suite.primitive;

public class IntDbl_DblRethrow {

	public static IntDbl_Dbl fun2(IntDbl_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
