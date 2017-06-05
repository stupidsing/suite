package suite.primitive;

public class ChrFlt_LngRethrow {

	public static ChrFlt_Lng fun2(ChrFlt_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
