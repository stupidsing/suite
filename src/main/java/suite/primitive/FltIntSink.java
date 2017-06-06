package suite.primitive;

public interface FltIntSink {

	public void sink2(float c, int f);

	public default FltIntSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
