package suite.primitive; import static suite.util.Friends.fail;

public interface LngChr_Dbl {

	public double apply(long c, char f);

	public default LngChr_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
