package suite.primitive;

public class FltFlt_LngRethrow {

	public static FltFlt_Lng fun2(FltFlt_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
