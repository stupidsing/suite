package suite.primitive;

public class ShtInt_IntRethrow {

	public static ShtInt_Int fun2(ShtInt_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
