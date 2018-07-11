package suite.primitive; import static suite.util.Friends.fail;

public interface FltInt_Lng {

	public long apply(float c, int f);

	public default FltInt_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
