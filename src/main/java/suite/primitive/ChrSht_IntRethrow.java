package suite.primitive;

public class ChrSht_IntRethrow {

	public static ChrSht_Int fun2(ChrSht_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
