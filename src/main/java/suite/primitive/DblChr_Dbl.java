package suite.primitive; import static suite.util.Friends.fail;

public interface DblChr_Dbl {

	public double apply(double c, char f);

	public default DblChr_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
