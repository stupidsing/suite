package suite.primitive;

public class ShtFlt_ShtRethrow {

	public static ShtFlt_Sht fun2(ShtFlt_Sht fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
