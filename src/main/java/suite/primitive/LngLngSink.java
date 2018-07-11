package suite.primitive; import static suite.util.Friends.fail;

public interface LngLngSink {

	public void sink2(long c, long f);

	public default LngLngSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
