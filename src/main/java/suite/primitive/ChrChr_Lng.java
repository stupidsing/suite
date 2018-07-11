package suite.primitive; import static suite.util.Friends.fail;

public interface ChrChr_Lng {

	public long apply(char c, char f);

	public default ChrChr_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
