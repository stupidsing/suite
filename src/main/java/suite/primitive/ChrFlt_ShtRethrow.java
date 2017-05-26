package suite.primitive;

public class ChrFlt_ShtRethrow {

	public static ChrFlt_Sht fun2(ChrFlt_Sht fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
