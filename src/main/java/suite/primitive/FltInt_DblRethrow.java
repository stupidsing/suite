package suite.primitive;

public class FltInt_DblRethrow {

	public static FltInt_Dbl fun2(FltInt_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
