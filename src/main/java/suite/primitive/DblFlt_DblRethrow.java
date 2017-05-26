package suite.primitive;

public class DblFlt_DblRethrow {

	public static DblFlt_Dbl fun2(DblFlt_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
