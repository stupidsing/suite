package suite.primitive;

public class FltChr_LngRethrow {

	public static FltChr_Lng fun2(FltChr_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
