package suite.primitive;

public class IntInt_LngRethrow {

	public static IntInt_Lng fun2(IntInt_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
