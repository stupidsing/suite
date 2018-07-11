package suite.primitive; import static suite.util.Friends.fail;

public interface LngFltSink {

	public void sink2(long c, float f);

	public default LngFltSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
