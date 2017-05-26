package suite.primitive;

public class ChrChr_ShtRethrow {

	public static ChrChr_Sht fun2(ChrChr_Sht fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
