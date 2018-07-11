package suite.primitive; import static suite.util.Friends.fail;

public interface LngDbl_Chr {

	public char apply(long c, double f);

	public default LngDbl_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
