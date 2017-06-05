package suite.primitive;

public class DblChr_LngRethrow {

	public static DblChr_Lng fun2(DblChr_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
