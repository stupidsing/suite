package suite.primitive;

public class ChrChr_LngRethrow {

	public static ChrChr_Lng fun2(ChrChr_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
