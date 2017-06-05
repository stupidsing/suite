package suite.primitive;

public class LngChr_IntRethrow {

	public static LngChr_Int fun2(LngChr_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
