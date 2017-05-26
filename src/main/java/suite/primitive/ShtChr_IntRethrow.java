package suite.primitive;

public class ShtChr_IntRethrow {

	public static ShtChr_Int fun2(ShtChr_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
