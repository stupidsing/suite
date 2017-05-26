package suite.primitive;

public class ShtSht_ShtRethrow {

	public static ShtSht_Sht fun2(ShtSht_Sht fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
