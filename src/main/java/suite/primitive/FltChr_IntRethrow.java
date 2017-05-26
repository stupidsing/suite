package suite.primitive;

public class FltChr_IntRethrow {

	public static FltChr_Int fun2(FltChr_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
