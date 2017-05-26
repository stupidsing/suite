package suite.primitive;

public class ChrChr_IntRethrow {

	public static ChrChr_Int fun2(ChrChr_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
