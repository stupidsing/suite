package suite.primitive;

public class DblInt_IntRethrow {

	public static DblInt_Int fun2(DblInt_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
