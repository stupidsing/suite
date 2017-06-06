package suite.primitive;

public interface FltLngSink {

	public void sink2(float c, long f);

	public default FltLngSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
