package suite.primitive;

public class FltFlt_ShtRethrow {

	public static FltFlt_Sht fun2(FltFlt_Sht fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
