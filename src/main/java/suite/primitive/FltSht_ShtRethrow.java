package suite.primitive;

public class FltSht_ShtRethrow {

	public static FltSht_Sht fun2(FltSht_Sht fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
