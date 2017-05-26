package suite.primitive;

public class ShtChr_ShtRethrow {

	public static ShtChr_Sht fun2(ShtChr_Sht fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
