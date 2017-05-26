package suite.primitive;

public class IntInt_DblRethrow {

	public static IntInt_Dbl fun2(IntInt_Dbl fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
