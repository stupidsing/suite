package suite.primitive;

public interface LngFltSink {

	public void sink2(long c, float f);

	public default LngFltSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
