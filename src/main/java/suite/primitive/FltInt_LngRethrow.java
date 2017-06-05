package suite.primitive;

public class FltInt_LngRethrow {

	public static FltInt_Lng fun2(FltInt_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
