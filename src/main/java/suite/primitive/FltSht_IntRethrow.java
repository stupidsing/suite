package suite.primitive;

public class FltSht_IntRethrow {

	public static FltSht_Int fun2(FltSht_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
