package suite.primitive;

public class IntFlt_ShtRethrow {

	public static IntFlt_Sht fun2(IntFlt_Sht fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
