package suite.primitive;

public class IntChr_LngRethrow {

	public static IntChr_Lng fun2(IntChr_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
