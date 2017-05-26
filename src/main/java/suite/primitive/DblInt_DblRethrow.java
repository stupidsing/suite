package suite.primitive;

public class DblInt_DblRethrow {

	public static DblInt_Dbl fun2(DblInt_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
