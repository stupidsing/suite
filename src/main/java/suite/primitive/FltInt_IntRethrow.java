package suite.primitive;

public class FltInt_IntRethrow {

	public static FltInt_Int fun2(FltInt_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
