package suite.primitive;

public class LngChr_LngRethrow {

	public static LngChr_Lng fun2(LngChr_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
