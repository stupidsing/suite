package suite.primitive;

public class ChrInt_IntRethrow {

	public static ChrInt_Int fun2(ChrInt_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
