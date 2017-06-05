package suite.primitive;

public class DblFlt_LngRethrow {

	public static DblFlt_Lng fun2(DblFlt_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
