package suite.primitive;

public class DblChr_IntRethrow {

	public static DblChr_Int fun2(DblChr_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
