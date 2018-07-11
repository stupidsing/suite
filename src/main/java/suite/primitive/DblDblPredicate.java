package suite.primitive; import static suite.util.Friends.fail;

public interface DblDblPredicate {

	public boolean test(double c, double f);

	public default DblDblPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
