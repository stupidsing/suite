package suite.primitive;

public class ChrDbl_DblRethrow {

	public static ChrDbl_Dbl fun2(ChrDbl_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
