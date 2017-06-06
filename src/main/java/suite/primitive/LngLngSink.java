package suite.primitive;

public interface LngLngSink {

	public void sink2(long c, long f);

	public default LngLngSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
