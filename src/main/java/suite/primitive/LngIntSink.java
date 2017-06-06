package suite.primitive;

public interface LngIntSink {

	public void sink2(long c, int f);

	public default LngIntSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
