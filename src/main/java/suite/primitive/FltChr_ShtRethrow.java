package suite.primitive;

public class FltChr_ShtRethrow {

	public static FltChr_Sht fun2(FltChr_Sht fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
