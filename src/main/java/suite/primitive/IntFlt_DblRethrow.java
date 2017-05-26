package suite.primitive;

public class IntFlt_DblRethrow {

	public static IntFlt_Dbl fun2(IntFlt_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
