package suite.primitive;

public class ShtSht_IntRethrow {

	public static ShtSht_Int fun2(ShtSht_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
