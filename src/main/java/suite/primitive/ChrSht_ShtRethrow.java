package suite.primitive;

public class ChrSht_ShtRethrow {

	public static ChrSht_Sht fun2(ChrSht_Sht fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
