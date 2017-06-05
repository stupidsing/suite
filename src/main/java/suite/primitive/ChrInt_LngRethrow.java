package suite.primitive;

public class ChrInt_LngRethrow {

	public static ChrInt_Lng fun2(ChrInt_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
