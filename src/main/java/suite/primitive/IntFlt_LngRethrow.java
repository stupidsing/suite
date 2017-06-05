package suite.primitive;

public class IntFlt_LngRethrow {

	public static IntFlt_Lng fun2(IntFlt_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
