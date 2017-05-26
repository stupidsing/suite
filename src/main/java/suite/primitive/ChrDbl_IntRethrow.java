package suite.primitive;

public class ChrDbl_IntRethrow {

	public static ChrDbl_Int fun2(ChrDbl_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
