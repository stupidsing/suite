package suite.primitive;

public class DblInt_LngRethrow {

	public static DblInt_Lng fun2(DblInt_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
